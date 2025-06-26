package com.webjing.issues.service.impl;

import com.webjing.issues.entity.ListedIssue;
import com.webjing.issues.entity.ListedIssueComment;
import com.webjing.issues.entity.ListedIssueLabel;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.query.IssueLabelQuery;
import com.webjing.issues.service.IssueLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;

/**
 * @description:
 * @className: IssueLabelServiceImpl
 * @author: webjing
 * @date: 2025年06月26日 09:53
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueLabelServiceImpl implements IssueLabelService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<ListedIssueLabel>> listIssueLabels(IssueLabelQuery query) {
        return client.listBy(IssueLabel.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueLabel)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    private Mono<ListedIssueLabel> toListedIssueLabel(IssueLabel issueLabel) {
        ListedIssueLabel.ListedIssueLabelBuilder issueBuilder = ListedIssueLabel.builder()
            .issueLabel(issueLabel);
        return Mono.just(issueBuilder)
            .map(ListedIssueLabel.ListedIssueLabelBuilder::build)
            .flatMap(lil -> fetchLabelSubIssueNum(issueLabel.getMetadata().getName())
                .doOnNext(lil::setIssueNumber)
                .thenReturn(lil));
    }

    private Mono<Integer> fetchLabelSubIssueNum(String labelName){
       return client.listAll(Issue.class, ListOptions.builder().fieldQuery(
                    QueryFactory.equal("spec.labels", labelName))
                .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
           .map(issues -> issues.size());
    }

}
