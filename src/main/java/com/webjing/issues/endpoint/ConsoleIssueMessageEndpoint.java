package com.webjing.issues.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import com.webjing.issues.extension.IssueMessage;
import com.webjing.issues.query.IssueMessageQuery;
import com.webjing.issues.service.IssueMessageService;
import com.webjing.issues.vo.ListedIssueMessage;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

/**
 * 控制台端的issue留言
 * @author: webjing
 * @date: 2025年03月06日 14:41
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleIssueMessageEndpoint implements CustomEndpoint {

    private final String tag = groupVersion() + "/IssueMessage";

    private final IssueMessageService issueMessageService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("issues", this::listIssueMessages, builder -> {
                builder.operationId("ListIssueMessages")
                    .description("List issueMessages.")
                    .tag(tag)
                    .response(responseBuilder()
                        .implementation(ListResult.generateGenericClass(ListedIssueMessage.class))
                    );
                IssueMessageQuery.buildParameters(builder);
            })
            .GET("issues/{name}", this::getIssueMessage,
                builder -> builder.operationId("GetIssueMessage")
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
                        .implementation(ListedIssueMessage.class)
                    ))
            .GET("labels", this::listMyLabels,
                builder -> builder.operationId("ListLabels")
                    .description("List all issue message labels.")
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
            .POST("issues", this::createMoment,
                builder -> builder.operationId("CreateIssueMessage")
                    .description("Create a IssueMessage.")
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
            .build();
    }

    private Mono<ServerResponse> getIssueMessage(ServerRequest request) {
        var name = request.pathVariable("name");
        return issueMessageService.findIssueMessageByName(name)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<ServerResponse> createMoment(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(IssueMessage.class)
            .map(issueMessage -> {
                issueMessage.getSpec().setApproved(true);
                issueMessage.getSpec().setApprovedTime(Instant.now());
                return issueMessage;
            })
            .flatMap(issueMessageService::create)
            .flatMap(issueMessage -> ServerResponse.ok().bodyValue(issueMessage));
    }

    private Mono<ServerResponse> listIssueMessages(ServerRequest serverRequest) {
        IssueMessageQuery query = new IssueMessageQuery(serverRequest.exchange());
        return issueMessageService.listIssueMessage(query)
            .flatMap(listedIssueMessages -> ServerResponse.ok().bodyValue(listedIssueMessages));
    }

    private Mono<ServerResponse> listMyLabels(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        return getCurrentUser()
            .map(username -> new IssueMessageQuery(request.exchange(), username))
            .flatMapMany(issueMessageService::listAllLabels)
            .filter(labelName -> StringUtils.isBlank(name) || StringUtils.containsIgnoreCase(labelName,
                name))
            .collectList()
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<String> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.issueMessage.webjing.com/v1alpha1");
    }

}
