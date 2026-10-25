package com.webjing.issues.endpoint.uc;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.entity.IssueStatusChangeParam;
import com.webjing.issues.entity.IssueTemplateRender;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueService;
import com.webjing.issues.service.IssuePermissionService;
import com.webjing.issues.service.IssueLabelService;
import com.webjing.issues.entity.IssueLabelOptions;
import java.util.List;
import java.util.HashSet;
import com.webjing.issues.service.IssueTemplateService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.AuthorityUtils;
import com.webjing.issues.entity.ListedIssue;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Set;
import lombok.Data;
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

    private final IssuePermissionService issuePermissionService;

    private final IssueLabelService issueLabelService;

    private final RoleService roleService;

    private final IssueTemplateService issueTemplateService;

    private final ReactiveExtensionClient client;

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
            .GET("issues/content", this::fetchIssueContent, builder ->
                builder.operationId("FetchIssueContent")
                    .description("fetch issue content.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("issueName")
                        .in(ParameterIn.QUERY)
                        .required(false)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(Issue.IssueContent.class)
                    )
            )
            .GET("issuetemplates/{templateName}", this::fetchIssueTemplateData, builder ->
                builder.operationId("FetchIssueTemplateData")
                    .description("fetch issue template.")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("templateName")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class)
                    )
                    .response(responseBuilder()
                        .implementation(IssueTemplateRender.class)
                    )
            )
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
            .GET("issues/{name}/labels", this::getIssueLabelOptions, builder -> builder
                .operationId("GetIssueLabelOptions").tag(tag)
                .parameter(parameterBuilder().name("name").in(ParameterIn.PATH).required(true).implementation(String.class))
                .response(responseBuilder().implementation(IssueLabelOptions.class)))
            .PUT("issues/{name}/labels", this::updateIssueLabels, builder -> builder
                .operationId("UpdateIssueLabels").tag(tag)
                .parameter(parameterBuilder().name("name").in(ParameterIn.PATH).required(true).implementation(String.class))
                .requestBody(requestBodyBuilder().required(true).implementation(IssueLabelsUpdateRequest.class))
                .response(responseBuilder().implementation(Issue.class)))
            .PUT("issuestatus", this::updateMyIssueStatus,
                builder -> builder.operationId("UpdateMyIssueStatus")
                    .description("Update a My Issue status.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(IssueStatusChangeParam.class))
                        ))
                    .response(responseBuilder()
                        .implementation(Issue.class))
            )
            .build();
    }

    private Mono<ServerResponse> listMyIssue(ServerRequest request) {
        return getCurrentUser()
            .map(user -> new IssueQuery(request.exchange(), user.getName()))
            .flatMap(issueService::listIssue)
            .flatMap(listedMoments -> ServerResponse.ok().bodyValue(listedMoments));
    }

    private Mono<ServerResponse> fetchIssueContent(ServerRequest request) {
        String issueName = request.queryParam("issueName").get();
        return issueService.getIssueContent(issueName)
            .flatMap(issueContent -> ServerResponse.ok().bodyValue(issueContent));
    }

    private Mono<ServerResponse> fetchIssueTemplateData(ServerRequest request) {
        String issueTemplate = request.pathVariable("templateName");
        return issueTemplateService.buildTemplateData(issueTemplate)
            .flatMap(templateData -> ServerResponse.ok().bodyValue(templateData));
    }

    private Mono<ServerResponse> deleteMyIssue(ServerRequest request) {
        var name = request.pathVariable("name");
        return  getMyIssueDetail(name)
            .flatMap(issue -> roleService.getCurrentUser()
                .flatMap(curUser -> {
                    var roles = AuthorityUtils.authoritiesToRoles(curUser.getAuthorities());
                    return roleService.joint(roles,
                            Set.of(AuthorityUtils.ISSUE_MESSAGE_MANAGEMENT_ROLE_NAME,
                                AuthorityUtils.SUPER_ROLE_NAME))
                        .flatMap(result -> {
                            if (result) {
                                return issueService.deleteBy(issue);
                            }else{
                                if (curUser.getName().equals(issue.getSpec().getOwner())) {
                                    return issueService.deleteBy(issue);
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only issue owner can delete it"));
                                }
                            }
                        });
                }))
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
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
                        // Every update needs to be re-reviewed. 暂时去掉 后续加入配置根据使用者情况自定义更新issue后是否重新审核
                        // newSpec.setApproved(false);
                    })
                    .flatMap(issueService::updateBy);
            })
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
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
            .flatMap(createdIssue -> ServerResponse.ok().bodyValue(createdIssue));
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

    private Mono<ServerResponse> updateMyIssueStatus(ServerRequest request) {
        return request.bodyToMono(IssueStatusChangeParam.class)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status request")))
            .flatMap(change -> {
                if (StringUtils.isBlank(change.getIssueName()) || change.getIssueState() == null) {
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issue name and state are required"));
                }
                return client.get(Issue.class, change.getIssueName())
                    .flatMap(issue -> roleService.getCurrentUser()
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)))
                        .flatMap(user -> issuePermissionService.canChangeStatus(
                            issue.getSpec().getOwner(), issue.getSpec().getAssignees(), user)
                            .flatMap(allowed -> {
                                if (!allowed) return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "Only the issue owner, assignee or issue manager can change its status"));
                                return switch (change.getIssueState()) {
                                    case CLOSED -> StringUtils.isBlank(change.getChangeComment())
                                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写关闭原因"))
                                        : issueService.closeIssue(issue, change.getChangeComment(), user.getName());
                                    case PROGRESS -> issueService.reopenIssue(issue, user.getName());
                                    case AWAIT -> issueService.setAwaitIssue(issue, user.getName());
                                };
                            })));
            })
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    private Mono<Issue> getManagedIssue(String name) {
        return issuePermissionService.canManage().flatMap(allowed -> allowed
            ? client.get(Issue.class, name)
            : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Issue management permission is required")));
    }

    private Mono<ServerResponse> getIssueLabelOptions(ServerRequest request) {
        return getManagedIssue(request.pathVariable("name"))
            .flatMap(issue -> issueLabelService.listSubjectIssueLabels(issue.getSpec().getSubjectName(), null))
            .flatMap(options -> ServerResponse.ok().bodyValue(options));
    }

    private Mono<ServerResponse> updateIssueLabels(ServerRequest request) {
        return getManagedIssue(request.pathVariable("name"))
            .flatMap(issue -> request.bodyToMono(IssueLabelsUpdateRequest.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing labels request")))
                .flatMap(body -> {
                    if (body.getLabels() == null) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Labels must be an array"));
                    return issueLabelService.listSubjectIssueLabels(issue.getSpec().getSubjectName(), null)
                        .flatMap(options -> {
                            var allowed = new HashSet<String>();
                            options.getIssueLabelOptions().forEach(option -> allowed.add(option.getValue()));
                            if (!allowed.containsAll(body.getLabels())) return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "标签不属于当前主体的可用范围"));
                            issue.getSpec().setLabels(new java.util.LinkedHashSet<>(body.getLabels()));
                            return client.update(issue);
                        });
                }))
            .flatMap(issue -> ServerResponse.ok().bodyValue(issue));
    }

    @Data
    public static class IssueLabelsUpdateRequest {
        private List<String> labels;
    }


    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("uc.api.issue.webjing.com/v1alpha1");
    }



}
