package com.webjing.issues.endpoint.uc;

import com.webjing.issues.entity.ListedIssue;
import com.webjing.issues.entity.ListedIssueComment;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.query.IssueCommentQuery;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueCommentService;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.AuthorityUtils;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import java.time.Instant;
import java.util.Set;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * @description:
 * @className: UcIssueCommentEndpoint
 * @author: webjing
 * @date: 2025年05月26日 16:54
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcIssueCommentEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueComment";

    private final IssueCommentService issueCommentService;

    private final RoleService roleService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issuecomments", this::listMyIssueComment, builder -> {
                builder.operationId("ListMyIssuesComment")
                    .description("List My issues comment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueComment.class))
                    );
                IssueQuery.buildParameters(builder);
            })
            .GET("issuecomments/{name}", this::getMyIssue,
                builder -> builder.operationId("GetMyIssueComment")
                    .description("Get a My Issue Comment.")
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
            .POST("issuecomments", this::createMyIssueComment,
                builder -> builder.operationId("CreateMyIssueComment")
                    .description("Create a My IssueComment.")
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
            .PUT("issuecomments/{name}", this::updateMyIssueComment,
                builder -> builder.operationId("UpdateMyIssueComment")
                    .description("Update a My IssueComment.")
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
            .DELETE("issuecomments/{name}", this::deleteMyIssueComment,
                builder -> builder.operationId("DeleteMyIssueComment")
                    .description("Delete a My Issue Comment.")
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

    private Mono<ServerResponse> deleteMyIssueComment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueCommentDetail(name)
            .flatMap(issueCommentService::deleteBy)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> updateMyIssueComment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueCommentDetail(name)
            .flatMap(oldIssueMessage -> {
                IssueComment.IssueCommentSpec oldSpec = oldIssueMessage.getSpec();

                return request.bodyToMono(IssueComment.class)
                    .doOnNext(newissueComment -> {
                        IssueComment.IssueCommentSpec newSpec = newissueComment.getSpec();
                        newSpec.setOwner(oldSpec.getOwner());
                        // Every update needs to be re-reviewed.
                        newSpec.setApproved(false);
                    })
                    .flatMap(issueCommentService::updateBy);
            })
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> getMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueCommentDetail(name)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<IssueComment> getMyIssueCommentDetail(String issueCommentName) {
        return getCurrentUser()
            .flatMap(user -> issueCommentService.getByUsername(issueCommentName, user.getName())
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The issue message was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyIssueComment(ServerRequest request) {
        return getCurrentUser()
            .flatMap(user -> request.bodyToMono(IssueComment.class)
                .flatMap(issueComment -> {
                    issueComment.getSpec().setApproved(false);
                    issueComment.getSpec().setOwner(user.getName());
                    var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_COMMENT_PUBLISH_APPROVAL_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .doOnNext(result -> {
                            if (result) {
                                // If it is a user with audit authority, there is no need to review.
                                issueComment.getSpec().setApproved(true);
                                issueComment.getSpec().setApprovedTime(Instant.now());
                             }
                        })
                        .thenReturn(issueComment);
                })
            )
            .flatMap(issueCommentService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMyIssueComment(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new IssueCommentQuery(request.exchange(), user.getName()))
            .flatMap(issueCommentService::listIssueComment)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<Authentication> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.issueComment.webjing.com/v1alpha1");
    }

}
