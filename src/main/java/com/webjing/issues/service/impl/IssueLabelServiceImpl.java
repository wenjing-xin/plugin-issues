package com.webjing.issues.service.impl;

import com.webjing.issues.entity.ListedIssueLabel;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.query.IssueLabelQuery;
import com.webjing.issues.service.IssueLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;

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
        boolean isGlobal = issueLabel.getSpec().getIsGlobal();

        // 构建重复检测查询
        Mono<Boolean> duplicateCheck;
        if (isGlobal) {
            // 全局标签：检测所有同名全局标签 - 修复布尔值类型
            duplicateCheck = client.listAll(IssueLabel.class,
                    ListOptions.builder()
                        .fieldQuery(QueryFactory.and(
                            QueryFactory.equal("spec.labelName", labelName),
                            QueryFactory.equal("spec.isGlobal", "true") // 使用布尔值 true 而不是字符串 "true"
                        )).build(),
                    Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
                .collectList()
                .map(list -> !list.isEmpty());
        } else {
            // 非全局标签：检测同主体下同名标签 - 修复查询条件
            duplicateCheck = client.listAll(IssueLabel.class,
                    ListOptions.builder()
                        .fieldQuery(QueryFactory.and(
                            QueryFactory.equal("spec.labelName", labelName),
                            QueryFactory.equal("spec.isGlobal", "false"),
                            QueryFactory.equal("spec.subjectName", subjectName)
                        )).build(),
                    Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
                .collectList()
                .map(list -> !list.isEmpty());
        }

        // 执行检测并创建
        return duplicateCheck.flatMap(exists -> {
            if (exists) {
                String errorMsg = isGlobal ?
                    "全局标签名称重复: " + labelName :
                    "主体内标签名称重复: " + labelName + " (主体: " + subjectName + ")";
                return Mono.error(new IllegalArgumentException(errorMsg));
            }
            return client.create(issueLabel);
        });
    }

    private Mono<ListedIssueLabel> toListedIssueLabel(IssueLabel issueLabel) {
        ListedIssueLabel.ListedIssueLabelBuilder issueBuilder = ListedIssueLabel.builder()
            .issueLabel(issueLabel);
        return Mono.just(issueBuilder)
            .map(ListedIssueLabel.ListedIssueLabelBuilder::build)
            .flatMap(lil -> fetchLabelSubIssueNum(issueLabel.getMetadata().getName())
                .doOnNext(lil::setIssueNumber)
                .thenReturn(lil));
    }

    private Mono<Integer> fetchLabelSubIssueNum(String labelName){
       return client.listAll(Issue.class, ListOptions.builder().fieldQuery(
                    QueryFactory.equal("spec.labels", labelName))
                .build(), Sort.by(Sort.Order.desc("metadata.creationTimestamp")))
            .collectList()
           .map(issues -> issues.size());
    }

}
