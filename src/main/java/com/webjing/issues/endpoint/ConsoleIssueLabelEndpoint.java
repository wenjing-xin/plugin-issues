package com.webjing.issues.endpoint;

import com.webjing.issues.entity.ListedIssueLabel;
import com.webjing.issues.entity.ListedIssueSubject;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.query.IssueLabelQuery;
import com.webjing.issues.query.IssueSubjectQuery;
import com.webjing.issues.service.IssueLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

/**
 * @description:
 * @className: ConsoleIssueLabelEndpoint
 * @author: webjing
 * @date: 2025年06月26日 09:47
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueLabelEndpoint  implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueLabel";

    private final IssueLabelService issueLabelService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issuelabels", this::listIssueLabels, builder -> {
                builder.operationId("ListIssueLabels")
                    .description("List IssueLabels.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueLabel.class))
                    );
                IssueLabelQuery.buildParameters(builder);
            })
            .build();
    }

    private Mono<ServerResponse> listIssueLabels(ServerRequest serverRequest) {
        IssueLabelQuery query = new IssueLabelQuery(serverRequest.exchange());
        return issueLabelService.listIssueLabels(query)
            .flatMap(listedIssueLabels -> ServerResponse.ok().bodyValue(listedIssueLabels));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issueLabel.webjing.com/v1alpha1");
    }

}
