package com.webjing.issues.finder.impl;

import com.webjing.issues.entity.IssueStats;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.util.HaloUtils;
import com.webjing.issues.util.MeterUtils;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.finder.IssueFinder;
import com.webjing.issues.vo.ContributorVO;
import com.webjing.issues.vo.IssueCommentVO;
import com.webjing.issues.vo.IssueLabelVO;
import com.webjing.issues.vo.IssueVO;
import com.webjing.issues.entity.Stats;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.*;
import run.halo.app.extension.index.query.Condition;
import run.halo.app.theme.finders.Finder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static run.halo.app.extension.index.query.Queries.all;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.empty;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 14:54
 */
@Finder("issuesFinder")
@RequiredArgsConstructor
public class IssueFinderImpl implements IssueFinder {

    public static final Predicate<Issue> FIXED_PREDICATE = issueMessage -> issueMessage.getSpec().getApproved() == Boolean.TRUE;

    public static final Condition FIXED_QUERY = equal("spec.approved", Boolean.TRUE.toString());

    private final ReactiveExtensionClient client;

    @Override
    public Flux<IssueVO> listAll() {
        var listOptions = ListOptions.builder()
            .fieldQuery(FIXED_QUERY)
            .build();
        return client.listAll(Issue.class, listOptions, defaultSort())
            .concatMap(this::getIssueVo);
    }

    @Override
    public Mono<ListResult<IssueVO>> list(Integer page, Integer size, String subjectName, String issueState) {
        var query = empty();
        if (StringUtils.isNoneBlank(issueState) && !"all".equals(issueState)) {
            query = and(query, equal("status.state", issueState));
        }
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageIssues(query, pageRequest, subjectName);
    }

    @Override
    public Flux<IssueVO> listBy(String label) {
        var listOptions = ListOptions.builder()
            .fieldQuery(and(FIXED_QUERY, equal("spec.labels", label)))
            .build();
        return client.listAll(Issue.class, listOptions, defaultSort())
            .concatMap(this::getIssueVo);
    }

    @Override
    public Mono<IssueVO> get(String issueName) {
        return client.get(Issue.class, issueName)
            .filter(FIXED_PREDICATE)
            .flatMap(this::getIssueVo);
    }

    @Override
    public Flux<IssueLabelVO> listAlllabels() {
        var listOptions = ListOptions.builder()
            .fieldQuery(and(all("spec.labels"), FIXED_QUERY))
            .build();
        return client.listAll(Issue.class, listOptions, defaultSort())
            .flatMapIterable(issueMessage -> {
                var labels = issueMessage.getSpec().getLabels();
                if (labels == null) {
                    return List.of();
                }
                return labels.stream()
                    .map(tag -> new IssueMessageLabelPair(tag, issueMessage.getMetadata().getName()))
                    .toList();
            })
            .groupBy(IssueMessageLabelPair::labelName)
            .concatMap(groupedFlux -> groupedFlux.count()
                .defaultIfEmpty(0L)
                .map(count -> IssueLabelVO.builder()
                    .name(groupedFlux.key())
                    .momentCount(count.intValue())
                    .permalink("/moments?tag=" + UriUtils.encode(groupedFlux.key(),
                        StandardCharsets.UTF_8))
                    .build()
                )
            );
    }

    @Override
    public Mono<ListResult<IssueVO>> listByLabel(int pageNum, Integer pageSize, String labelName, String subjectName) {
        var query = empty();
        if (StringUtils.isNoneBlank(labelName)) {
            query = and(query, equal("spec.labels", labelName));
        }
        var pageRequest = PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultSort());
        return pageIssues(query, pageRequest, subjectName);
    }

    @Override
    public Flux<IssueCommentVO> listAllIssueComments(String issueName) {
        ListOptions listOptions = ListOptions.builder().fieldQuery(and(
                equal("spec.approved", "true"),
                equal("spec.issueName", issueName)
            ))
            .build();
        return client.listAll(IssueComment.class, listOptions, Sort.by("metadata.creationTimestamp").ascending())
            .flatMap(this::getIssueCommentVo);
    }

    record IssueMessageLabelPair(String labelName, String issueMessageName){}

    private Mono<ListResult<IssueVO>> pageIssues(Condition additionalQuery, PageRequest page, String subjectName) {
        var query = FIXED_QUERY;
        if (additionalQuery != null) {
            query = and(query, additionalQuery);
        }
        if (subjectName != null) {
            query = and(query, equal("spec.subjectName", subjectName));
        }
        var listOptions = ListOptions.builder()
            .fieldQuery(query)
            .build();
        return client.listBy(Issue.class, listOptions, page)
            .flatMap(list -> Flux.fromStream(list.get())
                .concatMap(this::getIssueVo)
                .collectList()
                .map(momentVos -> new ListResult<>(list.getPage(), list.getSize(),
                    list.getTotal(), momentVos)
                )
            )
            .defaultIfEmpty(
                new ListResult<>(page.getPageNumber(), page.getPageSize(), 0L, List.of()));
    }

    static Sort defaultSort() {
        return Sort.by("spec.releaseTime").descending()
            .and(ExtensionUtil.defaultSort());
    }

    private Mono<IssueVO> getIssueVo(@Nonnull Issue issue) {
        IssueVO issueVo = IssueVO.from(issue);
        return Mono.just(issueVo)
            .flatMap(imv -> fetchIssueStats(issueVo)
                .doOnNext(imv::setIssueStats)
                .thenReturn(imv)
            )
            .flatMap(imv -> {
                String owner = imv.getSpec().getOwner();
                return client.fetch(User.class, owner)
                    .map(ContributorVO::from)
                    .doOnNext(imv::setContributorVo)
                    .thenReturn(imv);
            })
            .flatMap(imv -> {
                Set<String> labels = imv.getSpec().getLabels();
                return Flux.fromStream(labels.stream())
                    .flatMap(label -> client.fetch(IssueLabel.class, label))
                    .collectList()
                    .doOnNext(imv::setIssueLabels)
                    .thenReturn(imv);
            })
            .flatMap(imv -> {
                String templateName = imv.getSpec().getIssueTemplate();
                if (StringUtils.isBlank(templateName)) {
                    return Mono.just(imv); // 模板为空则直接跳过处理
                }
                return client.fetch(IssueTemplate.class, templateName)
                    .switchIfEmpty(Mono.empty()) // 模板不存在时跳过
                    .flatMap(issueTemplate -> {
                        List<Map<String, String>> templateData = new ArrayList<>();
                        Map<String, IssueTemplate.TemplateField> fields = issueTemplate.getSpec().getFields();
                        if (fields == null || fields.isEmpty()) {
                            return Mono.just(imv); // 没有字段定义也直接返回
                        }
                        for (Map.Entry<String, IssueTemplate.TemplateField> entry : fields.entrySet()) {
                            Map<String, String> itemData = new HashMap<>();
                            IssueTemplate.TemplateField field = entry.getValue();
                            itemData.put("title", field.getTitle());
                            itemData.put("label", field.getKey());
                            itemData.put("value", imv.getMetadata().getAnnotations().get(entry.getKey()));
                            itemData.put("type", field.getType().name());
                            if (field.getType().equals(IssueTemplate.TemplateFieldTypeEnum.TEXT_AREA)) {
                                itemData.put("rows", field.getRows().toString());
                            }
                            if (field.getType().equals(IssueTemplate.TemplateFieldTypeEnum.SELECT)) {
                                for(Map<String, String> option: field.getFieldOptions()){
                                    if(option.get("generateVal").equals(imv.getMetadata().getAnnotations().get(entry.getKey()))){
                                        itemData.put("selectLabel", option.get("label"));
                                    }
                                }
                                if(!itemData.containsKey("selectLabel")){
                                    itemData.put("selectLabel", "");
                                }
                            }
                            if (field.getType().equals(IssueTemplate.TemplateFieldTypeEnum.RADIO)) {
                                if(StringUtils.isNotEmpty(imv.getMetadata().getAnnotations().get(field.getKey()))){
                                    List<String> checkBoxArray = HaloUtils.convertStrToList(imv.getMetadata().getAnnotations().get(field.getKey()));
                                    String labels = "";
                                    for (String value : checkBoxArray) {
                                        for (Map<String, String> option : field.getFieldOptions()) {
                                            if (value.equals(option.get("generateVal"))) {
                                                labels += ("," + option.get("label"));
                                                break;
                                            }
                                        }
                                    }
                                    itemData.put("checkBoxLabels", labels);
                                }else{
                                    itemData.put("checkBoxLabels", "");
                                }
                            }
                            templateData.add(itemData);
                        }

                        imv.setTemplateData(templateData);
                        return Mono.just(imv);
                    })
                    .defaultIfEmpty(imv); // 如果模板不存在或出错，保留原始 imv
            })
            .defaultIfEmpty(issueVo);
    }

    private Mono<IssueCommentVO> getIssueCommentVo(@Nonnull IssueComment issueComment) {
        IssueCommentVO issueCommentVo = IssueCommentVO.from(issueComment);
        return Mono.just(issueCommentVo)
            .flatMap(imv -> fetchIssueCommentStats(issueComment)
                .doOnNext(imv::setStats)
                .thenReturn(imv)
            )
            .flatMap(imv -> setOwner(issueComment.getSpec().getOwner(), imv))
            .flatMap(imv -> {
                // 如果 quoteCommentUid 不为空，则获取被回复的评论及其用户信息
                if (StringUtils.isNotBlank(issueComment.getSpec().getQuoteCommentUid())) {
                    return client.fetch(IssueComment.class, issueComment.getSpec().getQuoteCommentUid())
                        .flatMap(quoteIssueComment ->
                            client.fetch(User.class, quoteIssueComment.getSpec().getOwner())
                                .map(ContributorVO::from)
                                .doOnNext(imv::setReplyToOwner)
                        )
                        .thenReturn(imv);
                }
                return Mono.just(imv);
            });
    }

    private Mono<Stats> fetchIssueCommentStats(IssueComment issueComment) {
        Assert.notNull(issueComment, "The issue must not be null.");
        String name = issueComment.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(IssueComment.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .build())
            .defaultIfEmpty(Stats.empty());
    }

    private Mono<IssueCommentVO> setOwner(String owner, IssueCommentVO issueCommentVO) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issueCommentVO::setContributorVo)
            .thenReturn(issueCommentVO);
    }

    private Mono<IssueStats> fetchIssueStats(IssueVO issueVo) {
        String issueName = issueVo.getMetadata().getName();

        // 保留原有 Counter 查询，用于 upvote 和 downvote
        Mono<IssueStats> counterStatsMono = client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, issueName))
            .map(counter -> IssueStats.builder()
                .visit(counter.getVisit())
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

    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }
}
