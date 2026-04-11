package com.webjing.issues.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @description: issue 数据统计
 * @className: IssueStats
 * @author: webjing
 * @date: 2025年05月19日 17:01
 */
@Data
@Builder
public class IssueStats {

    private Integer visit;

    private Integer upvote;

    private Integer downvote;

    private Integer totalIssueComment;

    private Integer approvedIssueComment;

    private Integer awaitApproveIssueComment;

    public static IssueStats empty() {
        return IssueStats.builder()
            .visit(0)
            .upvote(0)
            .downvote(0)
            .totalIssueComment(0)
            .approvedIssueComment(0)
            .awaitApproveIssueComment(0)
            .build();
    }

}
