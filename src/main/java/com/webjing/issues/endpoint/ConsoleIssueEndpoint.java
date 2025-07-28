package com.webjing.issues.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.entity.ListedIssue;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.service.SettingConfigGetter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 控制台的 issue API
 * @author: webjing
 * @date: 2025年03月06日 14:41
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/Issue";

    private final IssueService issueService;

    private final RoleService roleService;

    private final SettingConfigGetter settingConfigGetter;

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issues", this::listIssues, builder -> {
                builder.operationId("ListIssues")
                    .description("List issues.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssue.class))
                    );
                IssueQuery.buildParameters(builder);
            })
            .GET("issues/{name}", this::getIssueByName,
                builder -> builder.operationId("GetIssue")
                    .description("Get a issue message by name.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .description("IssueMessage name")
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(ListedIssue.class)
                    ))
            .GET("labels", this::listSubjectLabels,
                builder -> builder.operationId("ListSubjectLabels")
                    .description("List current issueSubject all issue labels.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("subjectName")
                        .in(ParameterIn.QUERY)
                        .description("subject name to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.QUERY)
                        .description("Label name to query")
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementationArray(String.class)
                    ))
            .POST("issues", this::createIssue,
                builder -> builder.operationId("CreateIssue")
                    .description("Create a Issue.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .PUT("issues", this::updateIssue,
                builder -> builder.operationId("UpdateIssue")
                    .description("update a Issue.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .PUT("issues/closed", this::closedIssue,
                builder -> builder.operationId("ClosedIssue")
                    .description("Closed the Issue")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("closedComment")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(String.class)
                    )
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder().implementation(Issue.class))
                    )
            .PUT("issues/reopen/{issueName}", this::reopenIssue,
                builder -> builder.operationId("ReopenIssue")
                    .description("Reopen a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("issueName")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .DELETE("issues/{name}", this::deleteIssue,
                builder -> builder.operationId("DeleteIssue")
                    .description("Delete a Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder().implementation(Issue.class))
            )
            .build();
    }

    private Mono<ServerResponse> getIssueByName(ServerRequest request) {
        var name = request.pathVariable("name");
        return issueService.findIssueByName(name)
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> createIssue(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Issue.class)
            .map(issue -> {
                issue.getSpec().setApproved(true);
                issue.getSpec().setApprovedTime(Instant.now());
                // 控制台端增加issue 自动生成链接
                issue.getStatus().setPermalink("/issues/" + issue.getMetadata().getName());
                return issue;
            })
            .flatMap(issueService::create)
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> updateIssue(ServerRequest serverRequest){
        return serverRequest.bodyToMono(Issue.class)
            .flatMap(issueService::consoleUpdateIssue)
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<ServerResponse> listIssues(ServerRequest serverRequest) {
        IssueQuery query = new IssueQuery(serverRequest.exchange());
        return issueService.listIssue(query)
            .flatMap(listedIssues -> ServerResponse.ok().bodyValue(listedIssues));
    }

    private Mono<ServerResponse> listSubjectLabels(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        IssueQuery issueQuery = new IssueQuery(request.exchange());
        return issueService.listAllLabels(issueQuery)
            .filter(labelName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(labelName, name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> closedIssue(ServerRequest request){
        return Mono.justOrEmpty(request.queryParam("closedComment"))
            .switchIfEmpty(Mono.defer(() ->
                settingConfigGetter.getIssuesBasic()
                    .map(issuesBasic -> issuesBasic.getDefaultClosedComment())
                    .onErrorResume(e -> Mono.just("默认关闭原因"))
            ))
            .flatMap(closedComment ->
                request.bodyToMono(Issue.class).flatMap(issue ->
                        roleService.getCurrentUser()
                            .flatMap(curUser ->
                                issueService.closeIssue(issue, closedComment, curUser.getName())
                            )
                    ).flatMap(updatedIssue ->
                        ServerResponse.ok().bodyValue(updatedIssue)
                    )
            );
    }

    private Mono<ServerResponse> reopenIssue(ServerRequest request) {
        String issueName = request.pathVariable("issueName");
        return client.get(Issue.class, issueName)
            .flatMap(issue -> roleService.getCurrentUser().flatMap(curUser -> issueService.reopenIssue(issue, curUser.getName())))
            .flatMap(updatedRes -> ServerResponse.ok().bodyValue(updatedRes));
    }

    private Mono<ServerResponse> deleteIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return client.get(Issue.class, name)
            .flatMap(issue -> issueService.deleteBy(issue))
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issue.webjing.com/v1alpha1");
    }

}
