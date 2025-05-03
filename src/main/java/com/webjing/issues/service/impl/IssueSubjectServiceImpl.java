package com.webjing.issues.service.impl;

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
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

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
            .flatMap(li -> fetchStats(issueSubject)
                .doOnNext(li::setStats)
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
     * 数据统计
     * @param issueSubject
     * @return
     */
    private Mono<Stats> fetchStats(IssueSubject issueSubject) {
        Assert.notNull(issueSubject, "The issueSubject must not be null.");
        String name = issueSubject.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .totalComment(counter.getTotalComment())
                .approvedComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(Stats.empty());
    }


}
