package com.webjing.issues;

import com.webjing.issues.finder.IssueFinder;
import com.webjing.issues.finder.IssueSubjectFinder;
import com.webjing.issues.service.RoleService;
import com.webjing.issues.service.SettingConfigGetter;
import com.webjing.issues.vo.IssueVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.halo.app.theme.router.PageUrlUtils.totalPage;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年01月05日 13:17
 */

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class IssuesRouter {

    private final SettingConfigGetter settingConfigGetter;

    private final IssueFinder issueFinder;

    private final IssueSubjectFinder issueSubjectFinder;

    private final TemplateNameResolver templateNameResolver;

    private final PluginContext pluginContext;

    private final RoleService roleService;

    @Bean
    RouterFunction<ServerResponse> issueRouterFunction() {
        return route(GET("/subject/{subjectName}"), this::handlerIssueSubjectFunction)
            .andRoute(GET("/subject/{subjectName}/issues").or(GET("/subject/{subjectName}/issues/page/{page:\\d+}")), this::handlerIssuePageFunction)
            .andRoute(GET("/subject/{subjectName}/issues/new"), this::newIssueRouter)
            .andRoute(GET("/subject/{subjectName}/issues/{issueName}"), this::issueDetailRouter);
    }

    private Mono<ServerResponse> issueDetailRouter(ServerRequest request) {
        final var issueName = request.pathVariable("issueName");
        final var subjectName = request.pathVariable("subjectName");
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),"issue")
            .flatMap(templateName -> {
                Map<String, Object> model = new HashMap<>(7);
                model.put("title",  getIssuesTitle());
                model.put("issueVO", issueFinder.get(issueName));
                model.put("issueSubjectInfo", issueSubjectFinder.getSubjectBasicInfo(subjectName));
                model.put("issueSubjectStats", issueSubjectFinder.getSubjectStats(subjectName));
                model.put("issueComments", issueFinder.listAllIssueComments(issueName));
                model.put("issueCosedComment", getIssueCosedComment());
                buildCommonVariables(model);
                return ServerResponse.ok().render(templateName, model);
            });
    }

    private Mono<ServerResponse> handlerIssueSubjectFunction(ServerRequest request) {
        final var subjectName = request.pathVariable("subjectName");
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),"subject")
            .flatMap(templateName -> {
                Map<String, Object> model = new HashMap<>(3);
                model.put("title",  getIssuesTitle());
                model.put("issueSubjectVO", issueSubjectFinder.get(subjectName));
                buildCommonVariables(model);
                return ServerResponse.ok().render(templateName, model);
            });
    }

    private Mono<ServerResponse> newIssueRouter(ServerRequest request){
        final var subjectName = request.pathVariable("subjectName");
        return roleService.getCurrentUser()
            .flatMap(curUser -> {
                if("anonymousUser".equals(curUser.getName())){
                    String loginUrl = "/login?redirect_uri=" + request.uri();
                    return ServerResponse.status(HttpStatus.FOUND)
                        .location(URI.create(loginUrl))
                        .build();
                }else {
                    return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),
                            "newIssue")
                        .flatMap(templateName -> {
                            Map<String, Object> model = new HashMap<>(3);
                            model.put("issueSubjectInfo", issueSubjectFinder.getSubjectBasicInfo(subjectName));
                            model.put("issueSubjectStats", issueSubjectFinder.getSubjectStats(subjectName));
                            buildCommonVariables(model);
                            return ServerResponse.ok().render(templateName, model);
                        });
                }
            }
        );
    }

    private Mono<ServerResponse> handlerIssuePageFunction(ServerRequest request) {
        final var subjectName = request.pathVariable("subjectName");
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),"issues")
            .flatMap(templateName -> {
                Map<String, Object> model = new HashMap<>(4);
                model.put("title",  getIssuesTitle());
                model.put("issueSubjectInfo", issueSubjectFinder.getSubjectBasicInfo(subjectName));
                model.put("issueSubjectStats", issueSubjectFinder.getSubjectStats(subjectName));
                model.put("issueItems", issuePageList(request, subjectName));
                buildCommonVariables(model);
                return ServerResponse.ok().render(templateName, model);
            });
    }

    private Mono<String> getIssuesTitle(){
        return settingConfigGetter.getIssuesBasic().map(issuesBasic -> issuesBasic.getTitle());
    }

    private Mono<String> getIssuesAvatarSetting(){
        return settingConfigGetter.getIssuesBasic().map(issuesBasic -> issuesBasic.getDefaultAvatarMode());
    }

    private Mono<String> getIssueCosedComment(){
        return settingConfigGetter.getIssuesBasic()
            .map(issuesBasic -> issuesBasic.getDefaultClosedComment())
            .defaultIfEmpty("Issue已解决");
    }

    private Mono<UrlContextListResult<IssueVO>> issuePageList(ServerRequest request, String subjectName) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);
        return settingConfigGetter.getIssuesBasic()
            .map(SettingConfigGetter.IssuesBasic::getPageSize)
            .defaultIfEmpty(10)
            .flatMap(pageSize -> issueFinder.list(pageNum, pageSize, subjectName)
                .map(list -> new UrlContextListResult.Builder<IssueVO>()
                    .listResult(list)
                    .nextUrl(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                    .prevUrl(PageUrlUtils.prevPageUrl(path))
                    .build()
                )
            );
    }

    private int pageNumInPathVariable(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

    /**
     * 构建一些通用变量
     * @param model
     */
    private void buildCommonVariables(Map<String, Object> model) {
        String version = pluginContext.getVersion();
        model.put("pluginVersion", version);
        model.put("issueAvatarMode", this.getIssuesAvatarSetting());
    }

}
