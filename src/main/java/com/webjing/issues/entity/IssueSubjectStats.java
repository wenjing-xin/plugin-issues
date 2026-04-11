package com.webjing.issues.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @description: Issue主体数据统计
 * @className: IssueSubjectStats
 * @author: webjing
 * @date: 2025年05月20日 09:03
 */
@Data
@Builder
public class IssueSubjectStats {

    private Integer totalIssue;

    private Integer progressIssue;

    private Integer awaitIssue;

    private Integer closedIssue;

    private Integer labels;

    private Integer awaitApproved;

    public static IssueSubjectStats empty() {
        return IssueSubjectStats.builder()
            .totalIssue(0)
            .progressIssue(0)
            .awaitIssue(0)
            .closedIssue(0)
            .labels(0)
            .build();
    }

}
