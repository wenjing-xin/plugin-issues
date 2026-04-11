package com.webjing.issues.entity;

import com.webjing.issues.extension.Issue;
import com.webjing.issues.vo.ContributorVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 11:30
 */
@Data
@Builder
public class ListedIssue {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Issue issue;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVO contributorVo;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueStats issueStats;
}
