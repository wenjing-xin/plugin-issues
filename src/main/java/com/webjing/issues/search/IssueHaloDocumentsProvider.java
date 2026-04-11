package com.webjing.issues.search;

import com.webjing.issues.Constant;
import com.webjing.issues.extension.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;

import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;
import run.halo.app.search.HaloDocument;
import run.halo.app.search.HaloDocumentsProvider;

/**
 * @description:
 * @className: IssueHaloDocumentsProvider
 * @author: webjing
 * @date: 2025年07月27日 23:18
 */
@Component
@RequiredArgsConstructor
public class IssueHaloDocumentsProvider implements HaloDocumentsProvider {

    private final ReactiveExtensionClient client;

    private final IssueDocumentConverter converter;

    @Override
    public Flux<HaloDocument> fetchAll() {
        var options = ListOptions.builder()
            .fieldQuery(isNull("metadata.deletionTimestamp"))
            .andQuery(equal("spec.approved", "true"))
            .build();
        var pageRequest = createPageRequest();
        // make sure the issues are approved and not deleted.
        return client.listBy(Issue.class, options, pageRequest)
            .map(ListResult::getItems)
            .flatMapMany(Flux::fromIterable)
            .flatMap(converter::convert);
    }

    @Override
    public String getType() {
        return Constant.ISSUE_DOCUMENT_TYPE;
    }

    private PageRequest createPageRequest() {
        return PageRequestImpl.of(1, Constant.SEARCH_DEFAULT_PAGE_SIZE,
            Sort.by("metadata.creationTimestamp", "metadata.name"));
    }


}
