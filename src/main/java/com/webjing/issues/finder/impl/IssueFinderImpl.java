package com.webjing.issues.finder.impl;

import com.webjing.issues.entity.IssueStats;
import com.webjing.issues.entity.ListedIssue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.IssueLabel;
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
import run.halo.app.extension.index.query.Query;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.theme.finders.Finder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static run.halo.app.extension.index.query.QueryFactory.*;

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

    public static final Query FIXED_QUERY = equal("spec.approved", Boolean.TRUE.toString());

    private final ReactiveExtensionClient client;

    @Override
    public Flux<IssueVO> listAll() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(
            FieldSelector.of(FIXED_QUERY));
        return client.listAll(Issue.class, listOptions, defaultSort())
            .concatMap(this::getIssueVo);
    }

    @Override
    public Mono<ListResult<IssueVO>> list(Integer page, Integer size, String subjectName) {
        var pageRequest = PageRequestImpl.of(pageNullSafe(page), sizeNullSafe(size), defaultSort());
        return pageIssues(null, pageRequest, subjectName);
    }

    @Override
    public Flux<IssueVO> listBy(String label) {
        var listOptions = new ListOptions();
        var query = and(FIXED_QUERY, equal("spec.labels", label));
        listOptions.setFieldSelector(FieldSelector.of(query));
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
        var listOptions = new ListOptions();
        var query = and(all("spec.tags"), FIXED_QUERY);
        listOptions.setFieldSelector(FieldSelector.of(query));
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
        var query = all();
        if (StringUtils.isNoneBlank(labelName)) {
            query = and(query, equal("spec.labels", labelName));
        }
        var pageRequest =
            PageRequestImpl.of(pageNullSafe(pageNum), sizeNullSafe(pageSize), defaultSort());
        return pageIssues(FieldSelector.of(query), pageRequest, subjectName);
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

    private Mono<ListResult<IssueVO>> pageIssues(FieldSelector fieldSelector, PageRequest page, String subjectName) {
        var listOptions = new ListOptions();
        var query = FIXED_QUERY;
        if (fieldSelector != null) {
            query = and(query, fieldSelector.query());
        }
        if(subjectName != null){
            query = and(query, equal("spec.subjectName", subjectName));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
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
            .defaultIfEmpty(issueVo);
    }

    private Mono<IssueCommentVO> getIssueCommentVo(@Nonnull IssueComment issueComment) {
        IssueCommentVO issueCommentVo = IssueCommentVO.from(issueComment);
        return Mono.just(issueCommentVo)
            .flatMap(imv -> fetchIssueCommentStats(issueComment)
                .doOnNext(imv::setStats)
                .thenReturn(imv)
            )
            .flatMap(imv -> setOwner(issueComment.getSpec().getOwner(), imv));
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

    private Mono<IssueStats> fetchIssueStats(IssueVO issueMessageVo) {
        String name = issueMessageVo.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, name))
            .map(counter -> IssueStats.builder()
                .visit(counter.getVisit())
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .totalIssueComment(counter.getTotalComment())
                .approvedIssueComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(IssueStats.empty());
    }

    int pageNullSafe(Integer page) {
        return ObjectUtils.defaultIfNull(page, 1);
    }

    int sizeNullSafe(Integer size) {
        return ObjectUtils.defaultIfNull(size, 10);
    }
}
