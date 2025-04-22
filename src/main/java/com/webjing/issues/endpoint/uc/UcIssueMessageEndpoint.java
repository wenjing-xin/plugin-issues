package com.webjing.issues.endpoint.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.extension.IssueMessage;
import com.webjing.issues.query.IssueMessageQuery;
import com.webjing.issues.service.IssueMessageService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.AuthorityUtils;
import com.webjing.issues.vo.ListedIssueMessage;
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
public class UcIssueMessageEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueMessage";

    private final IssueMessageService issueMessageService;

    private final RoleService roleService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issues", this::listMyMoment, builder -> {
                builder.operationId("ListMyIssues")
                    .description("List My issues.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueMessage.class))
                    );
                IssueMessageQuery.buildParameters(builder);
            })
            .GET("issues/{name}", this::getMyIssue,
                builder -> builder.operationId("GetMyIssue")
                    .description("Get a My IssueMessage.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(IssueMessage.class))
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
                                .implementation(IssueMessage.class))
                        ))
                    .response(responseBuilder()
                        .implementation(IssueMessage.class))
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
                                .implementation(IssueMessage.class))
                        ))
                    .response(responseBuilder()
                        .implementation(IssueMessage.class))
            )
            .DELETE("issues/{name}", this::deleteMyMoment,
                builder -> builder.operationId("DeleteMyIssue")
                    .description("Delete a My Issue.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("name")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder().implementation(IssueMessage.class))
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

    private Mono<ServerResponse> deleteMyMoment(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(issueMessageService::deleteBy)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> updateMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(oldIssueMessage -> {
                IssueMessage.IssueMessageSpec oldSpec = oldIssueMessage.getSpec();

                return request.bodyToMono(IssueMessage.class)
                    .doOnNext(newMoment -> {
                        IssueMessage.IssueMessageSpec newSpec = newMoment.getSpec();
                        newSpec.setOwner(oldSpec.getOwner());
                        newSpec.setReleaseTime(oldSpec.getReleaseTime());
                        // Every update needs to be re-reviewed.
                        newSpec.setApproved(false);
                    })
                    .flatMap(issueMessageService::updateBy);
            })
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> getMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return getMyIssueDetail(name)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<IssueMessage> getMyIssueDetail(String issueName) {
        return getCurrentUser()
            .flatMap(user -> issueMessageService.getByUsername(issueName, user.getName())
                .switchIfEmpty(
                    Mono.error(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The issue message was not found or deleted"))
                )
            );
    }

    private Mono<ServerResponse> createMyIssue(ServerRequest request) {
        return getCurrentUser()
            .flatMap(user -> request.bodyToMono(IssueMessage.class)
                .flatMap(issueMessage -> {
                    issueMessage.getSpec().setApproved(false);
                    issueMessage.getSpec().setOwner(user.getName());
                    var roles = AuthorityUtils.authoritiesToRoles(user.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_MESSAGE_PUBLISH_APPROVAL_ROLE_NAME,
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
            .flatMap(issueMessageService::create)
            .flatMap(moment -> ServerResponse.ok().bodyValue(moment));
    }

    private Mono<ServerResponse> listMyMoment(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new IssueMessageQuery(request.exchange(), user.getName()))
            .flatMap(issueMessageService::listIssueMessage)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<Authentication> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication);
    }

    private Mono<ServerResponse> listMyLabels(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(user -> new IssueMessageQuery(request.exchange(), user.getName()))
            .flatMapMany(issueMessageService::listAllLabels)
            .filter(tagName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(tagName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.issueMessage.webjing.com/v1alpha1");
    }

}
