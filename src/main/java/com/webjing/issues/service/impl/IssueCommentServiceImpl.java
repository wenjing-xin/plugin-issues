package com.webjing.issues.service.impl;

import com.webjing.issues.entity.ListedIssueComment;
import com.webjing.issues.entity.Stats;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.query.IssueCommentQuery;
import com.webjing.issues.service.IssueCommentService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.MeterUtils;
import com.webjing.issues.vo.ContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Counter;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import java.util.Objects;

/**
 * @description: issue 评论的相关接口实现类
 * @className: IssueCommentServiceImpl
 * @author: webjing
 * @date: 2025年05月26日 09:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueCommentServiceImpl implements IssueCommentService {

    private final ReactiveExtensionClient client;

    private final RoleService roleService;

    @Override
    public Mono<IssueComment> create(IssueComment issueComment) {
        return roleService.getContextUser()
            .flatMap(user -> {
                issueComment.getSpec().setOwner(user.getMetadata().getName());
                return client.create(issueComment);
            });
    }

    @Override
    public Mono<ListResult<ListedIssueComment>> listIssueComment(IssueCommentQuery query) {
        return client.listBy(IssueComment.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueComment)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<IssueComment> updateBy(IssueComment issue) {
        return client.update(issue);
    }

    @Override
    public Mono<IssueComment> deleteBy(IssueComment issue) {
        return client.delete(issue);
    }

    @Override
    public Mono<IssueComment> getByUsername(String issueCommentName, String name) {
        return client.get(IssueComment.class, issueCommentName)
            .filter(post -> post.getSpec() != null)
            .filter(post -> Objects.equals(name, post.getSpec().getOwner()));
    }

    @Override
    public Mono<IssueComment.IssueCommentContent> getIssueCommentContent(String issueCommentName) {
        return client.fetch(IssueComment.class, issueCommentName).map(issueComment -> issueComment.getSpec().getContent());
    }

    private Mono<ListedIssueComment> toListedIssueComment(IssueComment issueComment) {
        ListedIssueComment.ListedIssueCommentBuilder issueBuilder = ListedIssueComment.builder()
            .issueComment(issueComment);
        return Mono.just(issueBuilder)
            .map(ListedIssueComment.ListedIssueCommentBuilder::build)
            .flatMap(li -> fetchIssueCommentStats(issueComment)
                .doOnNext(li::setStats)
                .thenReturn(li))
            .flatMap(li -> setOwner(issueComment.getSpec().getOwner(), li));
    }

    private Mono<ListedIssueComment> setOwner(String owner, ListedIssueComment issue) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issue::setContributorVo)
            .thenReturn(issue);
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

}
