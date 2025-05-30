package com.webjing.issues.endpoint.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.AuthorityUtils;
import com.webjing.issues.entity.ListedIssue;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Set;
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
/**
 * 个人发布 issue 留言的 API
 * @author: webjing
 * @date: 2025年03月06日 14:42
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcIssueEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/Issue";

    private final IssueService issueService;

    private final RoleService roleService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issues", this::listMyIssue, builder -> {
                builder.operationId("ListMyIssues")
                    .description("List My issues.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssue.class))
                    );
                IssueQuery.buildParameters(builder);
            })
            .GET("issues/{name}", this::getMyIssue,
                builder -> builder.operationId("GetMyIssue")
                    .description("Get a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .POST("issues", this::createMyIssue,
                builder -> builder.operationId("CreateMyIssue")
                    .description("Create a My Issue.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(Issue.class))
                        ))
                    .response(responseBuilder().implementation(Issue.class))
            )
            .PUT("issues/{name}", this::updateMyIssue,
                builder -> builder.operationId("UpdateMyIssue")
                    .description("Update a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
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
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .DELETE("issues/{name}", this::deleteMyIssue,
                builder -> builder.operationId("DeleteMyIssue")
                    .description("Delete a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder().implementation(Issue.class))
            )
            .GET("labels", this::listMyLabels, builder -> builder.operationId("ListMyLabels")
                    .description("List all issue labels.")
                    .tag(tag)
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
            .build();
    }

    private Mono<ServerResponse> deleteMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(issueService::deleteBy)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> updateMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(oldIssue -> {
                Issue.IssueSpec oldSpec = oldIssue.getSpec();
                return request.bodyToMono(Issue.class)
                    .doOnNext(newIssue -> {
                        Issue.IssueSpec newSpec = newIssue.getSpec();
                        newSpec.setOwner(oldSpec.getOwner());
                        newSpec.setReleaseTime(oldSpec.getReleaseTime());
                        // Every update needs to be re-reviewed.
                        newSpec.setApproved(false);
                    })
                    .flatMap(issueService::updateBy);
            })
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> getMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<Issue> getMyIssueDetail(String issueName) {
        return getCurrentUser()
            .flatMap(user -> issueService.getByUsername(issueName, user.getName())
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The issue message was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyIssue(ServerRequest request) {
        return getCurrentUser()
            .flatMap(user -> request.bodyToMono(Issue.class)
                .flatMap(issueMessage -> {
                    issueMessage.getSpec().setApproved(false);
                    issueMessage.getSpec().setOwner(user.getName());
                    var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_PUBLISH_APPROVAL_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .doOnNext(result -> {
                            if (result) {
                                // If it is a user with audit authority, there is no need to review.
                                issueMessage.getSpec().setApproved(true);
                                issueMessage.getSpec().setApprovedTime(Instant.now());
                                // 拥有发布issue无需审核权限的时候 则自动生成访问链接
                                issueMessage.getStatus().setPermalink("/issues/" + issueMessage.getMetadata().getName());
                            }
                        })
                        .thenReturn(issueMessage);
                })
            )
            .flatMap(issueService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMyIssue(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new IssueQuery(request.exchange(), user.getName()))
            .flatMap(issueService::listIssue)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<Authentication> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication);
    }

    private Mono<ServerResponse> listMyLabels(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(user -> new IssueQuery(request.exchange(), user.getName()))
            .flatMapMany(issueService::listAllLabels)
            .filter(labelName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(labelName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.issue.webjing.com/v1alpha1");
    }

}
