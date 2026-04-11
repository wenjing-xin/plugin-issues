package com.webjing.issues.service.impl;

import com.webjing.issues.Constant;
import com.webjing.issues.entity.IssueStats;
import com.webjing.issues.entity.IssueTemplateOptions;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.finder.IssueSubjectFinder;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.MeterUtils;
import com.webjing.issues.exception.NotFoundException;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.util.ReasonDataConverterUtils;
import com.webjing.issues.vo.ContributorVO;
import com.webjing.issues.entity.ListedIssue;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static run.halo.app.extension.MetadataUtil.nullSafeAnnotations;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * Issue extensions for apis implemention
 * @author: webjing
 * @date: 2025年03月06日 14:54
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    private final NotificationReasonEmitter notificationReasonEmitter;

    private final ExternalLinkProcessor externalLinkProcessor;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Mono<ListResult<ListedIssue>> listIssue(IssueQuery query) {
        return client.listBy(Issue.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssue)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<Issue> create(Issue issue) {
        if (Objects.isNull(issue.getSpec().getReleaseTime())) {
            issue.getSpec().setReleaseTime(Instant.now());
        }

        return roleService.getContextUser()
            .flatMap(user -> {
                issue.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issue);
            });
    }

    @Override
    public Flux<String> listAllLabels(IssueQuery query) {
        return client.listAll(Issue.class, query.toListOptions(),
                Sort.by("metadata.name").descending())
            .flatMapIterable(issue -> {
                var labels = issue.getSpec().getLabels();
                return Objects.requireNonNullElseGet(labels, List::of);
            })
            .distinct();
    }

    @Override
    public Mono<ListedIssue> findIssueByName(String name) {
        return client.fetch(Issue.class, name)
            .switchIfEmpty(Mono.error(new NotFoundException("Issue not found.")))
            .flatMap(this::toListedIssue);
    }

    @Override
    public Mono<Issue> getByUsername(String issueName, String username) {
        return client.get(Issue.class, issueName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> Objects.equals(username, post.getSpec().getOwner()));
    }

    @Override
    public Mono<Issue> updateBy(Issue issue) {
        return client.update(issue);
    }

    @Override
    public Mono<Issue> deleteBy(Issue issue) {
        return client.listAll(IssueComment.class, ListOptions.builder()
                    .fieldQuery(equal("spec.issueName", issue.getMetadata().getName())).build(),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
            .flatMapMany(Flux::fromIterable)
            .flatMap(comment -> client.delete(comment))
            .then(client.delete(issue));
    }

    private Mono<ListedIssue> toListedIssue(Issue issue) {
        ListedIssue.ListedIssueBuilder issueBuilder = ListedIssue.builder()
            .issue(issue);
        return Mono.just(issueBuilder)
            .map(ListedIssue.ListedIssueBuilder::build)
            .flatMap(li -> fetchIssueStats(issue)
                .doOnNext(li::setIssueStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issue.getSpec().getOwner(), li));
    }

    private Mono<ListedIssue> setOwner(String owner, ListedIssue issue) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issue::setContributorVo)
            .thenReturn(issue);
    }

    private Mono<IssueStats> fetchIssueStats(Issue issue) {
        Assert.notNull(issue, "The issue must not be null.");
        String issueName = issue.getMetadata().getName();

        // 保留原有 Counter 查询，用于 upvote 和 downvote
        Mono<IssueStats> counterStatsMono = client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, issueName))
            .map(counter -> IssueStats.builder()
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .build())
            .defaultIfEmpty(IssueStats.builder().upvote(0).downvote(0).build());

        // 新增 IssueComment 查询，用于统计评论
        Mono<IssueStats> commentStatsMono = client.listAll(IssueComment.class, ListOptions.builder()
                    .fieldQuery(equal("spec.issueName", issueName)).build(),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
            .map(comments -> {
                int totalComment = comments.size();
                long approvedComment = comments.stream()
                    .filter(comment -> Boolean.TRUE.equals(comment.getSpec().getApproved()))
                    .count();
                long awaitApprovedComment = comments.stream()
                    .filter(comment -> Boolean.FALSE.equals(comment.getSpec().getApproved()))
                    .count();
                return IssueStats.builder()
                    .totalIssueComment(totalComment)
                    .approvedIssueComment((int) approvedComment)
                    .awaitApproveIssueComment((int) awaitApprovedComment)
                    .build();
            });

        // 合并两个结果
        return Mono.zip(counterStatsMono, commentStatsMono)
            .map(tuple -> {
                IssueStats counterStats = tuple.getT1();
                IssueStats commentStats = tuple.getT2();
                return IssueStats.builder()
                    .visit(counterStats.getVisit())
                    .upvote(counterStats.getUpvote())
                    .downvote(counterStats.getDownvote())
                    .totalIssueComment(commentStats.getTotalIssueComment())
                    .approvedIssueComment(commentStats.getApprovedIssueComment())
                    .awaitApproveIssueComment(commentStats.getAwaitApproveIssueComment())
                    .build();
            });
    }


    @Override
    public Mono<Issue> closeIssue(Issue issue, String closedComment, String closedOwner) {
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.CLOSED);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(closedOwner);
        stateTransition.setComment(closedComment);
        issue.getStatus().getTransitions().add(stateTransition);
        // 设置状态为关闭
        issue.getStatus().setState(Issue.IssueState.CLOSED);
        issue.getSpec().setClosedAt(Instant.now());
        // 更新状态
        var issueAnnotations = nullSafeAnnotations(issue);
        var newIssueNotified = issueAnnotations.getOrDefault(Constant.CLOSED_ISSUE_NOTIFIED_ANNO,"false");
        if (Objects.equals(newIssueNotified,"false")) {
            Set<String> issueWatchers = new HashSet<>(issue.getSpec().getAssignees());
            issueWatchers.add(issue.getSpec().getOwner());
            return client.fetch(IssueSubject.class, issue.getSpec().getSubjectName())
                .map(issueSubject -> {
                    String issueSubjectTypeName = IssueSubject.parseSubjectType(issueSubject.getSpec()
                        .getSubjectType());
                    return IssueSubjectInfo.builder()
                       .subjectDisplayName(issueSubject.getSpec().getDisplayName())
                       .subjectType(issueSubjectTypeName)
                        .build();
                }).flatMap(issueSubjectInfo -> {
                    //添加已经通知的标识
                    issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "true");
                    return client.update(issue).flatMap(updatedIssue ->this.sendClosedIssueNotification(updatedIssue, issueWatchers, issueSubjectInfo.subjectDisplayName,
                        issueSubjectInfo.subjectType, closedComment, closedOwner).thenReturn(updatedIssue));
                });
        }
        return client.update(issue);
    }

    @Override
    public Mono<Issue.IssueContent> getIssueContent(String issueName) {
        return client.fetch(Issue.class, issueName).map(issue -> issue.getSpec().getContent());
    }

    @Override
    public Mono<Issue> reopenIssue(Issue issue, String reopenOwner) {
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.PROGRESS);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(reopenOwner);
        stateTransition.setComment("重新打开Issue");
        issue.getStatus().getTransitions().add(stateTransition);
        // 设置状态为关闭
        issue.getStatus().setState(Issue.IssueState.PROGRESS);

        // 更新状态
        var issueAnnotations = nullSafeAnnotations(issue);
        issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "false");
        return client.update(issue);
    }

    @Override
    public Mono<Issue> setAwaitIssue(Issue issue, String setAwaitOwner){
        Issue.StateTransition stateTransition = new Issue.StateTransition();
        Issue.IssueState oldState = issue.getStatus().getState();
        stateTransition.setFromState(oldState);
        stateTransition.setToState(Issue.IssueState.AWAIT);
        stateTransition.setTime(Instant.now());
        stateTransition.setOperator(setAwaitOwner);
        stateTransition.setComment("设置Issue为等待中");
        issue.getStatus().getTransitions().add(stateTransition);
        // 设置状态为等待中
        issue.getStatus().setState(Issue.IssueState.AWAIT);
        if(oldState.equals(Issue.IssueState.CLOSED)){
            var issueAnnotations = nullSafeAnnotations(issue);
            issueAnnotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "false");
        }
        return client.update(issue);
    }

    /**
     * console端更新issue
     * @param issue
     * @return
     */
    @Override
    public Mono<Issue> consoleUpdateIssue(Issue issue){
        return client.fetch(Issue.class, issue.getMetadata().getName())
            .doOnNext(oldIssue -> {
                // 比对下Issue的经办人
                Set<String> oldAssignees = oldIssue.getSpec().getAssignees();
                Set<String> newAssignees = issue.getSpec().getAssignees();
                // 找出新增的assignees
                if (newAssignees != null && newAssignees.size() > 0) {
                    Set<String> addedAssignees = new HashSet<>(newAssignees);
                    if (oldAssignees != null && oldAssignees.size() > 0) {
                        addedAssignees.removeAll(oldAssignees);
                    }
                    for (String addedAssignee : addedAssignees) {
                        notificationSubscriptionHelper.reactiveSubscribeComment(UserIdentity.of(addedAssignee));
                    }
                }
            })
            .flatMap(oldIssue -> client.update(issue));
    }

    /**
     * 列出当前主体下可供选择的模版
     * @param subjectName
     * @return
     */
    @Override
    public Mono<IssueTemplateOptions> listIssueSelectTemplateOptions(String subjectName) {
        return client.fetch(IssueSubject.class, subjectName)
            .flatMap(issueSubject ->  Flux.fromIterable(issueSubject.getSpec().getIssueTemplates())
                .flatMap(templateName -> client.fetch(IssueTemplate.class, templateName)
                    .map(issueTemplate -> IssueTemplateOptions.IssueTemplateItem.from(issueTemplate))
                ).collectList()
                .map(templates -> {
                    IssueTemplateOptions options = new IssueTemplateOptions();
                    options.setIssueTemplateOptions(templates);
                    return options;
                })
            );
    }

    /**
     * 关闭issue的时候发送通知
     * @param issue
     * @param closedComment
     * @param closedOwner
     * @return
     */
    private Mono<Void> sendClosedIssueNotification(Issue issue, Set<String> participateUsers, String subjectDisplayName, String subjectType, String closedComment, String closedOwner) {
        Boolean approved = issue.getSpec().getApproved();
        String contentUrl;
        if(approved){
            contentUrl = externalLinkProcessor.processLink(issue.getStatus().getPermalink());
        }else{
            contentUrl = externalLinkProcessor.processLink("/console/issueSubject/issues?subjectName=" + issue.getSpec().getSubjectName() + "&approved=false");
        }
        return Flux.fromIterable(participateUsers).flatMap(participateUser -> {
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(issue.getApiVersion())
                .kind(issue.getKind())
                .name(issue.getMetadata().getName())
                .title(issue.getSpec().getTitle())
                .url(contentUrl)
                .build();
            String owner = issue.getSpec().getOwner();
            var emitReasonMono = notificationReasonEmitter.emit(Constant.MANAGER_CLOSED_ISSUE,
                builder -> {
                    var attributes = IssueClosedReasonData.builder()
                        .issueTitle(issue.getSpec().getTitle())
                        .issueClosedTime(issue.getSpec().getClosedAt()
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                        .closedComment(closedComment)
                        .issuePermalink(contentUrl)
                        .issueOwner(issue.getSpec().getOwner())
                        .closedOwner(closedOwner)
                        .receiveOwner(participateUser)
                        .subjectDisplayName(subjectDisplayName)
                        .subjectType(subjectType)
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(owner))
                        .subject(reasonSubject);
                });
            Mono<Void> subscribeMono = notificationSubscriptionHelper.subscribeClosedIssueNotify(UserIdentity.of(participateUser));
            return Mono.when(subscribeMono).then(emitReasonMono);
        }).then();

    }

    @Builder
    record IssueClosedReasonData(String issueTitle, String issueClosedTime, String closedComment, String issuePermalink, String issueOwner,String closedOwner,
                                 String receiveOwner, String subjectDisplayName, String subjectType) {
    }

    @Builder
    record IssueSubjectInfo(String subjectDisplayName, String subjectType){
    }

}
