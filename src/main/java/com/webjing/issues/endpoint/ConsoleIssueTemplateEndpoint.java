package com.webjing.issues.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.query.IssueTemplateQuery;
import com.webjing.issues.service.IssueTemplateService;
import com.webjing.issues.vo.ListedIssueTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月17日 11:32
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueTemplateEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueTemplate";

    private final IssueTemplateService issueTemplateService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issuetemplates", this::listIssueTemplates, builder -> {
                builder.operationId("ListIssueTemplates")
                    .description("List issueTemplates.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueTemplate.class))
                    );
                IssueTemplateQuery.buildParameters(builder);
            })
            .POST("issuetemplates", this::createIssueTemplate, builder ->
                builder.operationId("CreateIssueTemplate")
                    .description("create issue template.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder().implementation(IssueTemplate.class))
                        ))
                    .response(responseBuilder()
                        .implementation(IssueTemplate.class))
            )
            .build();
    }

    private Mono<ServerResponse> listIssueTemplates(ServerRequest serverRequest) {
        IssueTemplateQuery query = new IssueTemplateQuery(serverRequest.exchange());
        return issueTemplateService.listIssueTemplate(query)
            .flatMap(listedIssueTemplate -> ServerResponse.ok().bodyValue(listedIssueTemplate));
    }

    /**
     * 创建 issue 留言模版
     * @param request
     * @return
     */
    private Mono<ServerResponse> createIssueTemplate(ServerRequest request) {
        return request.bodyToMono(IssueTemplate.class)
            .flatMap(issueTemplateService::create)
            .flatMap(issueTemplate -> ServerResponse.ok().bodyValue(issueTemplate));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issueTemplate.webjing.com/v1alpha1");
    }
}
