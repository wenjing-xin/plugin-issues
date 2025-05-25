package com.webjing.issues.service.impl;

import com.webjing.issues.entity.IssueSubjectStats;
import com.webjing.issues.entity.ListedIssueSubject;
import com.webjing.issues.entity.Stats;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.query.IssueSubjectQuery;
import com.webjing.issues.service.IssueSubjectService;
import com.webjing.issues.util.MeterUtils;
import com.webjing.issues.vo.ContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public Mono<IssueSubject> create(IssueSubject issueSubject) {
        return getContextUser()
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

    protected Mono<User> getContextUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var name = ctx.getAuthentication().getName();
                return client.fetch(User.class, name);
            });
    }

    private Mono<ListedIssueSubject> toListedIssueSubject(IssueSubject issueSubject) {
        ListedIssueSubject.ListedIssueSubjectBuilder issueSubjectBuilder = ListedIssueSubject.builder()
            .issueSubject(issueSubject);
        return Mono.just(issueSubjectBuilder)
            .map(ListedIssueSubject.ListedIssueSubjectBuilder::build)
            .flatMap(li -> fetchIssueSubjectStats(issueSubject)
                .doOnNext(li::setIssueSubjectStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueSubject.getSpec().getOwner(), li));
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
            .doOnNext(issueSubject::setContributorVo)
            .thenReturn(issueSubject);
    }

    /**
     * issue主体数据统计
     * @param issueSubject
     * @return
     */
    private Mono<IssueSubjectStats> fetchIssueSubjectStats(IssueSubject issueSubject) {
        Assert.notNull(issueSubject, "The issueSubject must not be null.");
        String issueSubjectName = issueSubject.getMetadata().getName();

        return client.listAll(Issue.class, ListOptions.builder().fieldQuery(QueryFactory.equal("spec.subjectName", issueSubjectName))
                    .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
            .map(issues -> {
                int progress = (int) issues.stream()
                    .filter(i -> Issue.IssueState.PROGRESS == i.getStatus().getState() && i.getSpec()
                        .getApproved())
                    .count();
                int await = (int) issues.stream()
                    .filter(i -> Issue.IssueState.AWAIT == i.getStatus().getState() && i.getSpec().getApproved())
                    .count();
                int closed = (int) issues.stream()
                    .filter(i -> Issue.IssueState.CLOSED == i.getStatus().getState() && i.getSpec().getApproved())
                    .count();
                int awaitApproved = (int) issues.stream()
                    .filter(i -> !i.getSpec().getApproved())
                    .count();

                // 新增标签统计逻辑
                Set<String> uniqueLabels = issues.stream()
                    .filter(issue -> issue.getSpec().getLabels() != null)
                    .flatMap(issue -> issue.getSpec().getLabels().stream())
                    .collect(Collectors.toSet());
                return IssueSubjectStats.builder()
                    .totalIssue(issues.size())
                    .progressIssue(progress)
                    .awaitIssue(await)
                    .closedIssue(closed)
                    .awaitApproved(awaitApproved)
                    .labels(uniqueLabels.size())
                    .build();
            })
            .defaultIfEmpty(IssueSubjectStats.empty());
    }



}
