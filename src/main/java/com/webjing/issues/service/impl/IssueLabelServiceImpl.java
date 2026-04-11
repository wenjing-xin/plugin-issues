package com.webjing.issues.service.impl;

import com.webjing.issues.entity.IssueLabelOptions;
import com.webjing.issues.entity.ListedIssueLabel;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.query.IssueLabelQuery;
import com.webjing.issues.service.IssueLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * @description:
 * @className: IssueLabelServiceImpl
 * @author: webjing
 * @date: 2025年06月26日 09:53
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueLabelServiceImpl implements IssueLabelService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<ListedIssueLabel>> listIssueLabels(IssueLabelQuery query) {
        return client.listBy(IssueLabel.class, query.toListOptions(), query.toPageRequest())
            .flatMap(listResult -> Flux.fromStream(listResult.get())
                .concatMap(this::toListedIssueLabel)
                .collectList()
                .map(list -> new ListResult<>(listResult.getPage(), listResult.getSize(),
                    listResult.getTotal(), list)
                )
            );
    }

    @Override
    public Mono<IssueLabel> create(IssueLabel issueLabel) {
        // 获取标签名称和主体名称
        String labelName = issueLabel.getSpec().getLabelName();
        String subjectName = issueLabel.getSpec().getSubjectName();
        String subjectType = issueLabel.getSpec().getSubjectType().name();
        IssueLabel.LabelScope tagScope = issueLabel.getSpec().getScope();
        // 构建重复检测查询
        Mono<Boolean> duplicateCheck;
        if (tagScope.name().equals("GLOBAL")) {
            // 全局标签：检测所有同名全局标签 - 修复布尔值类型
            duplicateCheck = client.listAll(IssueLabel.class,
                    ListOptions.builder()
                        .fieldQuery(and(
                            equal("spec.labelName", labelName),
                            equal("spec.scope", "GLOBAL")
                        )).build(),
                    Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
                .collectList()
                .map(list -> !list.isEmpty());
        }else if(tagScope.name().equals("SUBJECT_TYPE")){
            // 针对某一主体类型增加
            duplicateCheck = client.listAll(IssueLabel.class,
                    ListOptions.builder()
                        .fieldQuery(and(
                            equal("spec.labelName", labelName),
                            equal("spec.scope", "SUBJECT_TYPE"),
                            equal("spec.subjectType", subjectType)
                        )).build(),
                    Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
                .collectList()
                .map(list -> !list.isEmpty());
        } else {
            // 非全局标签：检测同主体下同名标签 - 修复查询条件
            duplicateCheck = client.listAll(IssueLabel.class,
                    ListOptions.builder()
                        .fieldQuery(and(
                            equal("spec.labelName", labelName),
                            equal("spec.scope", "SUBJECT"),
                            equal("spec.subjectName", subjectName)
                        )).build(),
                    Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
                .collectList()
                .map(list -> !list.isEmpty());
        }

        // 执行检测并创建
        return duplicateCheck.flatMap(exists -> {
            if (exists) {
                return switch (tagScope){
                    case GLOBAL ->  Mono.error(new IllegalArgumentException("全局标签名称重复: " + labelName));
                    case SUBJECT_TYPE -> Mono.error(new IllegalArgumentException("主体类型【" + IssueSubject.parseSubjectType(issueLabel.getSpec().getSubjectType()) + "】内标签名称重复: " + labelName));
                    case SUBJECT -> client.fetch(IssueSubject.class, subjectName).flatMap(issueSubject -> Mono.error(new IllegalArgumentException("主体【" + issueSubject.getSpec().getDisplayName() + "】内标签名称重复: " + labelName)));
                };
            }
            return client.create(issueLabel);
        });
    }

    @Override
    public Mono<IssueLabelOptions> listSubjectIssueLabels(String subjectName, String keyword) {
        // 查询全局标签 (isGlobal = true)
        Flux<IssueLabel> globalLabels = client.listAll(IssueLabel.class,
            buildQueryParam("GLOBAL", "", "", keyword),
            Sort.by(Sort.Order.desc("metadata.creationTimestamp"))
        ).map(issueLabel -> {
            String labelName = issueLabel.getSpec().getLabelName();
            issueLabel.getSpec().setLabelName(labelName + " - 全局标签");
            return issueLabel;
        });

        // 查询指定主体类型的标签 (isGlobal = false 且 subjectType 匹配)
        Flux<IssueLabel> subjectTypeLabels = client.get(IssueSubject.class, subjectName)
            .flatMapMany(issueSubject -> client.listAll(IssueLabel.class, 
                buildQueryParam("SUBJECT_TYPE", issueSubject.getSpec().getSubjectType().name(), "", keyword),
                Sort.by(Sort.Order.desc("metadata.creationTimestamp"))));

        // 查询指定主体的标签 (isGlobal = false 且 subjectName 匹配)
        Flux<IssueLabel> subjectLabels = client.listAll(IssueLabel.class,
            buildQueryParam("SUBJECT", "", subjectName, keyword),
            Sort.by(Sort.Order.desc("metadata.creationTimestamp"))
        );

        // 合并三个结果流并转换为 IssueLabelOptions
        return Flux.merge(globalLabels, subjectTypeLabels, subjectLabels)
            .map(issueLabel -> IssueLabelOptions.IssueLabelItem.from(issueLabel))
            .collectList()
            .map(issueLabelItems -> {
                IssueLabelOptions issueLabelOptions = new IssueLabelOptions();
                issueLabelOptions.setIssueLabelOptions(issueLabelItems);
                return issueLabelOptions;
            });
    }

    private Mono<ListedIssueLabel> toListedIssueLabel(IssueLabel issueLabel) {
        ListedIssueLabel.ListedIssueLabelBuilder issueBuilder = ListedIssueLabel.builder()
            .issueLabel(issueLabel);
        return Mono.just(issueBuilder)
            .map(ListedIssueLabel.ListedIssueLabelBuilder::build)
            .flatMap(lil -> fetchLabelSubIssueNum(issueLabel.getMetadata().getName())
                .doOnNext(lil::setIssueNumber)
                .thenReturn(lil))
            .flatMap(lil -> {
                if(lil.getIssueLabel().getSpec().getScope().name().equals("SUBJECT")){
                    return client.fetch(IssueSubject.class, lil.getIssueLabel().getSpec().getSubjectName())
                        .map(issueSubject -> issueSubject.getSpec().getDisplayName())
                        .doOnNext(lil::setSubjectDisplayName)
                        .thenReturn(lil);
                }
                return Mono.just(lil);
            });
    }

    private Mono<Integer> fetchLabelSubIssueNum(String labelName){
       return client.listAll(Issue.class, ListOptions.builder().fieldQuery(
                    equal("spec.labels", labelName))
                .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
           .map(issues -> issues.size());
    }

    private ListOptions buildQueryParam(String labelScope, String subjectType, String subjectName, String keyword){
        if(StringUtils.isEmpty(keyword)){
            if(labelScope.equals("GLOBAL")){
                return ListOptions.builder()
                    .fieldQuery(equal("spec.scope", labelScope))
                    .build();
            }else if(labelScope.equals("SUBJECT_TYPE")){
                return ListOptions.builder()
                    .fieldQuery(and(
                        equal("spec.scope", labelScope),
                        equal("spec.subjectType", subjectType)
                    ))
                    .build();
            }
            return ListOptions.builder()
                .fieldQuery(and(
                    equal("spec.scope", labelScope),
                    equal("spec.subjectName", subjectName)
                ))
                .build();
        }else{
            if(labelScope.equals("GLOBAL")){
                return ListOptions.builder()
                    .fieldQuery(and(
                        equal("spec.scope", labelScope),
                        equal("spec.labelName", keyword)
                    ))
                    .build();
            }else if(labelScope.equals("SUBJECT_TYPE")){
                return ListOptions.builder()
                    .fieldQuery(and(
                        equal("spec.scope", labelScope),
                        equal("spec.subjectType", subjectType),
                        equal("spec.labelName", keyword)
                    ))
                    .build();
            }
            return ListOptions.builder().fieldQuery(and(
                    equal("spec.scope", labelScope),
                    equal("spec.subjectName", subjectName),
                    equal("spec.labelName", keyword)
                ))
                .build();
        }
    }

}
