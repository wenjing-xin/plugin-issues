package com.webjing.issues.service.impl;

import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.query.IssueTemplateQuery;
import com.webjing.issues.service.IssueTemplateService;
import com.webjing.issues.vo.ContributorVo;
import com.webjing.issues.vo.ListedIssueTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月17日 11:40
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueTemplateServiceImpl implements IssueTemplateService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<IssueTemplate> create(IssueTemplate issueTemplate) {
        return client.create(issueTemplate);
    }

    @Override
    public Mono<ListResult<ListedIssueTemplate>> listIssueTemplate(IssueTemplateQuery query) {
        return client.listBy(IssueTemplate.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueTemplate)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    private Mono<ListedIssueTemplate> toListedIssueTemplate(IssueTemplate issueTemplate) {
        ListedIssueTemplate.ListedIssueTemplateBuilder templateBuilder = ListedIssueTemplate.builder()
            .issueTemplate(issueTemplate);
        return Mono.just(templateBuilder)
            .map(ListedIssueTemplate.ListedIssueTemplateBuilder::build)
            .flatMap(li -> setOwner(issueTemplate.getSpec().getOwner(), li));
    }

    private Mono<ListedIssueTemplate> setOwner(String owner, ListedIssueTemplate issueTemplate) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVo.from(user))
            .doOnNext(issueTemplate::setContributorVo)
            .thenReturn(issueTemplate);
    }

}
