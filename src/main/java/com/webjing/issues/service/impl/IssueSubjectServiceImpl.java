package com.webjing.issues.service.impl;

import com.webjing.issues.entity.IssueSubjectStats;
import com.webjing.issues.entity.ListedIssueSubject;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import com.webjing.issues.query.IssueSubjectQuery;
import com.webjing.issues.service.IssueSubjectService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.service.IssueLabelService;
import com.webjing.issues.vo.ContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.notification.UserIdentity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

import static run.halo.app.extension.index.query.Queries.equal;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年05月03日 18:23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueSubjectServiceImpl implements IssueSubjectService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    private final IssueLabelService issueLabelService;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Mono<IssueSubject> create(IssueSubject issueSubject) {
        return roleService.getContextUser()
            .flatMap(user -> {
                issueSubject.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issueSubject);
            });
    }

    @Override
    public Mono<ListResult<ListedIssueSubject>> listIssueSubject(IssueSubjectQuery query) {
        return client.listBy(IssueSubject.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueSubject)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    private Mono<ListedIssueSubject> toListedIssueSubject(IssueSubject issueSubject) {
        ListedIssueSubject.ListedIssueSubjectBuilder issueSubjectBuilder = ListedIssueSubject.builder()
            .issueSubject(issueSubject);
        return Mono.just(issueSubjectBuilder)
            .map(ListedIssueSubject.ListedIssueSubjectBuilder::build)
            .flatMap(li -> fetchIssueSubjectStats(issueSubject.getMetadata().getName())
                .doOnNext(li::setIssueSubjectStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueSubject.getSpec().getOwner(), li))
            .flatMap(li -> setParticipateUsers(Objects.requireNonNullElse(issueSubject.getSpec().getParticipateUsers(), List.of()), li));
    }

    /**
     * 设置归属者
     * @param owner
     * @param issueSubject
     * @return
     */
    private Mono<ListedIssueSubject> setOwner(String owner, ListedIssueSubject issueSubject) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issueSubject::setCreateOwner)
            .thenReturn(issueSubject);
    }

    /**
     * 查询参与用户信息
     * @param participateUsers
     * @param issueSubject
     * @return
     */
    private Mono<ListedIssueSubject> setParticipateUsers(List<String> participateUsers, ListedIssueSubject issueSubject){
        return Flux.fromIterable(participateUsers)
            .flatMap(participateUser -> client.fetch(User.class, participateUser)
                .map(user -> ContributorVO.from(user))
            )
            .collectList()
            .doOnNext(issueSubject::setParticipateUsers)
            .thenReturn(issueSubject);
    }

    /**
     * issue主体数据统计
     * @param issueSubjectName
     * @return
     */
    @Override
    public Mono<IssueSubjectStats> fetchIssueSubjectStats(String issueSubjectName) {
        Assert.notNull(issueSubjectName, "The issueSubject must not be null.");

        return client.listAll(Issue.class, ListOptions.builder().fieldQuery(equal("spec.subjectName", issueSubjectName))
                    .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .filter(issue -> issue.getMetadata().getDeletionTimestamp() == null)
            .collectList()
            .map(issues -> {
                int progress = (int) issues.stream()
                    .filter(i -> i.getStatus() != null && Issue.IssueState.PROGRESS == i.getStatus().getState() && Boolean.TRUE.equals(i.getSpec().getApproved()))
                    .count();
                int await = (int) issues.stream()
                    .filter(i -> i.getStatus() != null && Issue.IssueState.AWAIT == i.getStatus().getState() && Boolean.TRUE.equals(i.getSpec().getApproved()))
                    .count();
                int closed = (int) issues.stream()
                    .filter(i -> i.getStatus() != null && Issue.IssueState.CLOSED == i.getStatus().getState() && Boolean.TRUE.equals(i.getSpec().getApproved()))
                    .count();
                int awaitApproved = (int) issues.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.getSpec().getApproved()))
                    .count();

                return IssueSubjectStats.builder()
                    .totalIssue(progress + await + closed)
                    .progressIssue(progress)
                    .awaitIssue(await)
                    .closedIssue(closed)
                    .awaitApproved(awaitApproved)
                    .labels(0)
                    .build();
            })
            .flatMap(stats -> issueLabelService.listSubjectIssueLabels(issueSubjectName, null)
                .map(options -> {
                    stats.setLabels(options.getIssueLabelOptions().size());
                    return stats;
                }))
            .defaultIfEmpty(IssueSubjectStats.empty());
    }

    @Override
    public Mono<IssueSubject> updateIssueSubject(IssueSubject issueSubject) {
        return client.fetch(IssueSubject.class, issueSubject.getMetadata().getName())
            .doOnNext(oldIssueSubject -> {
                // 比对下Issue主体的的参与者
                Set<String> oldParticipateUsers = new HashSet<>(Objects.requireNonNullElse(oldIssueSubject.getSpec().getParticipateUsers(), List.of()));
                Set<String> newParticipateUsers = new HashSet<>(Objects.requireNonNullElse(issueSubject.getSpec().getParticipateUsers(), List.of()));
                // 找出新增的参与人
                if (newParticipateUsers != null && newParticipateUsers.size() > 0) {
                    Set<String> addedParticipateUsers = new HashSet<>(newParticipateUsers);
                    if (oldParticipateUsers != null && oldParticipateUsers.size() > 0) {
                        addedParticipateUsers.removeAll(oldParticipateUsers);
                    }
                    for (String addedAssignee : addedParticipateUsers) {
                        // 在这里处理新增的assignee
                        notificationSubscriptionHelper.reactiveSubscribeNewIssue(UserIdentity.of(addedAssignee));
                    }
                }
            })
            .flatMap(oldIssue -> client.update(issueSubject));
    }


}
