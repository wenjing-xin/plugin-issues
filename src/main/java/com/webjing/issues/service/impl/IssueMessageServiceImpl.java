package com.webjing.issues.service.impl;

import com.webjing.issues.MeterUtils;
import com.webjing.issues.exception.NotFoundException;
import com.webjing.issues.extension.IssueMessage;
import com.webjing.issues.query.IssueMessageQuery;
import com.webjing.issues.service.IssueMessageService;
import com.webjing.issues.vo.ContributorVo;
import com.webjing.issues.vo.ListedIssueMessage;
import com.webjing.issues.vo.Stats;
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
 * IssueMessage extensions for apis implemention
 * @author: webjing
 * @date: 2025年03月06日 14:54
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueMessageServiceImpl implements IssueMessageService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<ListedIssueMessage>> listIssueMessage(IssueMessageQuery query) {
        return client.listBy(IssueMessage.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueMessage)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<IssueMessage> create(IssueMessage issueMessage) {
        if (Objects.isNull(issueMessage.getSpec().getReleaseTime())) {
            issueMessage.getSpec().setReleaseTime(Instant.now());
        }

        return getContextUser()
            .flatMap(user -> {
                issueMessage.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issueMessage);
            });
    }

    @Override
    public Flux<String> listAllLabels(IssueMessageQuery query) {
        return client.listAll(IssueMessage.class, query.toListOptions(),
                Sort.by("metadata.name").descending())
            .flatMapIterable(moment -> {
                var tags = moment.getSpec().getLabels();
                return Objects.requireNonNullElseGet(tags, List::of);
            })
            .distinct();
    }

    @Override
    public Mono<ListedIssueMessage> findIssueMessageByName(String name) {
        return client.fetch(IssueMessage.class, name)
            .switchIfEmpty(Mono.error(new NotFoundException("Issue not found.")))
            .flatMap(this::toListedIssueMessage);
    }

    @Override
    public Mono<IssueMessage> getByUsername(String issueMessageName, String username) {
        return client.get(IssueMessage.class, issueMessageName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> Objects.equals(username, post.getSpec().getOwner()));
    }

    @Override
    public Mono<IssueMessage> updateBy(IssueMessage issueMessage) {
        return client.update(issueMessage);
    }

    @Override
    public Mono<IssueMessage> deleteBy(IssueMessage issueMessage) {
        return client.delete(issueMessage);
    }

    private Mono<ListedIssueMessage> toListedIssueMessage(IssueMessage issueMessage) {
        ListedIssueMessage.ListedIssueMessageBuilder momentBuilder = ListedIssueMessage.builder()
            .issueMessage(issueMessage);
        return Mono.just(momentBuilder)
            .map(ListedIssueMessage.ListedIssueMessageBuilder::build)
            .flatMap(li -> fetchStats(issueMessage)
                .doOnNext(li::setStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueMessage.getSpec().getOwner(), li));
    }

    private Mono<ListedIssueMessage> setOwner(String owner, ListedIssueMessage issueMessage) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVo.from(user))
            .doOnNext(issueMessage::setContributorVo)
            .thenReturn(issueMessage);
    }

    private Mono<Stats> fetchStats(IssueMessage issueMessage) {
        Assert.notNull(issueMessage, "The issueMessage must not be null.");
        String name = issueMessage.getMetadata().getName();
        return client.fetch(Counter.class, MeterUtils.nameOf(IssueMessage.class, name))
            .map(counter -> Stats.builder()
                .upvote(counter.getUpvote())
                .totalComment(counter.getTotalComment())
                .approvedComment(counter.getApprovedComment())
                .build())
            .defaultIfEmpty(Stats.empty());

    }

    protected Mono<User> getContextUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var name = ctx.getAuthentication().getName();
                return client.fetch(User.class, name);
            });
    }

}
