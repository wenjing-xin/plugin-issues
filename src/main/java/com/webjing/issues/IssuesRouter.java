package com.webjing.issues;

import com.webjing.issues.finder.IssueFinder;
import com.webjing.issues.service.SettingConfigGetter;
import com.webjing.issues.vo.IssueVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;

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
@Component
@RequiredArgsConstructor
public class IssuesRouter {

    private final SettingConfigGetter settingConfigGetter;

    private final IssueFinder issueMessageFinder;

    private final TemplateNameResolver templateNameResolver;

    @Bean
    RouterFunction<ServerResponse> selectedRouterFunction() {
        return route(GET("/issues/{name}"), this::issueDetailRouter)
            .andRoute(GET("/issues").or(GET("/issues/page/{page:\\d+}")), this::handlerIssuePageFunction);
    }

    private Mono<ServerResponse> issueDetailRouter(ServerRequest request) {
        final var name = request.pathVariable("name");
        return null;
        // return getPasteContentVo(name)
        //     .doOnNext(pasteShareContent -> {
        //         if(!pasteShareContent.getSpec().getPublish()){
        //             throw new NotFoundException("PasteShareContent not publish.");
        //         }
        //     })
        //     .switchIfEmpty(
        //         Mono.error(() -> new NotFoundException("PasteShareContent not found.")))
        //     .flatMap(pasteShareContent -> {
        //         Map<String, Object> model = new HashMap<>(7);
        //         return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),
        //                 "pasteShare")
        //             .flatMap(templateName -> ServerResponse.ok()
        //                 .render(templateName, setTemplateDatas(model, pasteShareContent)));
        //     });
    }


    private Mono<ServerResponse> handlerIssuePageFunction(ServerRequest request) {

        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(),"pasteShareIndex")
            .flatMap(templateName -> {
                Map<String, Object> model = new HashMap<>(2);
                model.put("title",  getIssuesTitle());
                model.put("issueItems", issuePageList(request));
                return ServerResponse.ok().render(templateName, model);
            });
    }

    private Mono<String> getIssuesTitle(){
        return settingConfigGetter.getIssuesBasic().map(issuesBasic -> issuesBasic.getTitle());
    }

    private Mono<UrlContextListResult<IssueVO>> issuePageList(ServerRequest request) {
        String path = request.path();
        int pageNum = pageNumInPathVariable(request);

        return settingConfigGetter.getIssuesBasic()
            .map(SettingConfigGetter.IssuesBasic::getPageSize)
            .defaultIfEmpty(10)
            .flatMap(pageSize -> issueMessageFinder.list(pageNum, pageSize)
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

}
