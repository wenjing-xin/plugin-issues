package com.webjing.issues.service.impl;

import com.webjing.issues.entity.IssueLabelOptions;
import com.webjing.issues.entity.IssueTemplateOptions;
import com.webjing.issues.entity.IssueTemplateRender;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.query.IssueTemplateQuery;
import com.webjing.issues.service.IssueTemplateService;
import com.webjing.issues.vo.ContributorVO;
import com.webjing.issues.entity.ListedIssueTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import java.util.Map;

import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * issue 模版功能
 * @author: webjing
 * @date: 2025年03月17日 11:40
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueTemplateServiceImpl implements IssueTemplateService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<IssueTemplate> create(IssueTemplate issueTemplate) {
        return client.create(issueTemplate);
    }

    @Override
    public Mono<ListResult<ListedIssueTemplate>> listIssueTemplate(IssueTemplateQuery query) {
        return client.listBy(IssueTemplate.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueTemplate)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<IssueTemplateOptions> listIssueTemplateOptions(String subjectTypeName, String subjectName) {
        // 查询特定主体类型的模版
        Flux<IssueTemplate> specialTypeTemplates = client.listAll(IssueTemplate.class,
            ListOptions.builder().fieldQuery(and(
                    equal("spec.scope", IssueTemplate.IssueTemplateScope.SUBJECT_TYPE.name()),
                    equal("spec.subjectType", subjectTypeName)))
                .build(),
            Sort.by(Sort.Order.desc("metadata.creationTimestamp"))
        ).map(issueTemplate -> {
            String templateName = issueTemplate.getSpec().getName();
            issueTemplate.getSpec().setName(templateName + " - " + IssueSubject.parseSubjectType(IssueSubject.SubjectType.valueOf(subjectTypeName)));
            return issueTemplate;
        });

        // 查询指定主体类型的模版
        Flux<IssueTemplate> specialSubjectTemplates = client.listAll(IssueTemplate.class,
            ListOptions.builder().fieldQuery(and(
                    equal("spec.scope", IssueTemplate.IssueTemplateScope.SUBJECT.name()),
                    equal("spec.subjectName", subjectName)))
                .build(),
            Sort.by(Sort.Order.desc("metadata.creationTimestamp")));

        // 合并两个结果流并转换为 IssueLabelOptions
        return Flux.merge(specialTypeTemplates, specialSubjectTemplates)
            .map(issueTemplate -> IssueTemplateOptions.IssueTemplateItem.from(issueTemplate))
            .collectList()
            .map(issueLabelItems -> {
                IssueTemplateOptions issueTemplateOptions = new IssueTemplateOptions();
                issueTemplateOptions.setIssueTemplateOptions(issueLabelItems);
                return issueTemplateOptions;
            });
    }

    /**
     * 构建前端组件渲染的 数据
     * @param templateName
     * @return
     */
    @Override
    public Mono<IssueTemplateRender> buildTemplateData(String templateName) {
        return client.get(IssueTemplate.class, templateName).map(issueTemplate -> {
            IssueTemplateRender issueTemplateRender = new IssueTemplateRender();
            issueTemplateRender.setDisplayName(issueTemplate.getSpec().getName());
            issueTemplateRender.setComponents(issueTemplate.getSpec().getFields().values().stream()
                .toList());
            issueTemplateRender.setAnnotationFields(issueTemplate.getSpec().getFields().entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .toList());
            return issueTemplateRender;
        });
    }

    private Mono<ListedIssueTemplate> toListedIssueTemplate(IssueTemplate issueTemplate) {
        ListedIssueTemplate.ListedIssueTemplateBuilder templateBuilder = ListedIssueTemplate.builder()
            .issueTemplate(issueTemplate);
        return Mono.just(templateBuilder)
            .map(ListedIssueTemplate.ListedIssueTemplateBuilder::build)
            .flatMap(li -> setOwner(issueTemplate.getSpec().getOwner(), li))
            .flatMap(li -> {
                if(li.getIssueTemplate().getSpec().getScope().name().equals("SUBJECT")){
                    return client.fetch(IssueSubject.class, li.getIssueTemplate().getSpec().getSubjectName())
                        .map(issueSubject -> issueSubject.getSpec().getDisplayName())
                        .doOnNext(li::setSubjectDisplayName)
                        .thenReturn(li);
                }
                return Mono.just(li);
            });
    }

    private Mono<ListedIssueTemplate> setOwner(String owner, ListedIssueTemplate issueTemplate) {
        return client.fetch(User.class, owner)
            .map(user -> ContributorVO.from(user))
            .doOnNext(issueTemplate::setContributorVo)
            .thenReturn(issueTemplate);
    }

}
