package com.webjing.issues.endpoint;

import com.webjing.issues.entity.ListedIssueSubject;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.query.IssueSubjectQuery;
import com.webjing.issues.query.IssueTemplateQuery;
import com.webjing.issues.service.IssueSubjectService;
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

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * issue依托主体对象接口
 * @author: webjing
 * @date: 2025年05月03日 18:15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueSubjectEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueSubject";

    private final IssueSubjectService issueSubjectService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issuesubjects", this::listIssueSubjects, builder -> {
                builder.operationId("ListIssueSubjects")
                    .description("List issueSubjects.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueSubject.class))
                    );
                IssueSubjectQuery.buildParameters(builder);
            })
            .POST("issuesubjects", this::createIssueSubject, builder ->
                builder.operationId("CreateIssueSubject")
                    .description("create issue subject.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder().implementation(IssueSubject.class))
                        ))
                    .response(responseBuilder()
                        .implementation(IssueSubject.class))
            )
            .build();
    }

    private Mono<ServerResponse> listIssueSubjects(ServerRequest serverRequest) {
        IssueSubjectQuery query = new IssueSubjectQuery(serverRequest.exchange());
        return issueSubjectService.listIssueSubject(query)
            .flatMap(listedIssueSubjects -> ServerResponse.ok().bodyValue(listedIssueSubjects));
    }

    /**
     * 创建 issue 依托主体
     * @param request
     * @return
     */
    private Mono<ServerResponse> createIssueSubject(ServerRequest request) {
        return request.bodyToMono(IssueSubject.class)
            .flatMap(issueSubjectService::create)
            .flatMap(issueTemplate -> ServerResponse.ok().bodyValue(issueTemplate));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issueSubject.webjing.com/v1alpha1");
    }
}
