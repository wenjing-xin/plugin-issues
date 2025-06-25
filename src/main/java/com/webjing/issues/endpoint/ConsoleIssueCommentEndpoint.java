package com.webjing.issues.endpoint;

import com.webjing.issues.entity.ListedIssueComment;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.query.IssueCommentQuery;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.service.IssueCommentService;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.util.HaloUtils;
import com.webjing.issues.util.IpAddressUtils;
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
import java.time.Instant;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * @description:
 * @className: ConsoleIssueCommentEndpoint
 * @author: webjing
 * @date: 2025年05月26日 11:11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueCommentEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/Issue";

    private final IssueCommentService issueCommentService;

    private final RoleService roleService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issuecomments", this::listIssueComment, builder -> {
                builder.operationId("ListIssuesComment")
                    .description("List current issues comment.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueComment.class))
                    );
                IssueCommentQuery.buildParameters(builder);
            })
            .POST("issuecomments", this::createIssueComment,
                builder -> builder.operationId("CreateIssueComment")
                    .description("Create a IssueComment.")
                    .tag(tag)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(IssueComment.class))
                        ))
                    .response(responseBuilder()
                        .implementation(IssueComment.class))
            )
            .build();
    }

    private Mono<ServerResponse> listIssueComment(ServerRequest request) {
        return issueCommentService.listIssueComment(new IssueCommentQuery(request.exchange()))
            .flatMap(listedIssueComments -> ServerResponse.ok().bodyValue(listedIssueComments));
    }

    private Mono<ServerResponse> createIssueComment(ServerRequest request) {
        return roleService.getCurrentUser()
            .flatMap(curUser -> request.bodyToMono(IssueComment.class)
                .map(issueComment -> {
                    issueComment.getSpec().setApproved(true);
                    issueComment.getSpec().setApprovedTime(Instant.now());
                    issueComment.getSpec().setIpAddress(IpAddressUtils.getIpAddress(request));
                    issueComment.getSpec().setUserAgent(HaloUtils.userAgentFrom(request));
                    return issueComment;
                }))
            .flatMap(issueCommentService::create)
            .flatMap(issueComment -> ServerResponse.ok().bodyValue(issueComment));
    }


    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issueComment.webjing.com/v1alpha1");
    }

}
