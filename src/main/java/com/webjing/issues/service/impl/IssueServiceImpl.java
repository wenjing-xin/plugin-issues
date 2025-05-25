package com.webjing.issues.service.impl;

import com.webjing.issues.entity.IssueStats;
import com.webjing.issues.util.MeterUtils;
import com.webjing.issues.exception.NotFoundException;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.vo.ContributorVO;
import com.webjing.issues.entity.ListedIssue;
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
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

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

        return getContextUser()
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
        return client.delete(issue);
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
        String name = issue.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(Issue.class, name))
            .map(counter -> IssueStats.builder()
                .upvote(counter.getUpvote())
                .downvote(counter.getDownvote())
                .totalIssueComment(counter.getTotalComment())
                .approvedIssueComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(IssueStats.empty());

    }

    protected Mono<User> getContextUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var name = ctx.getAuthentication().getName();
                return client.fetch(User.class, name);
            });
    }

}
