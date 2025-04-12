package com.webjing.issues.vo;

import com.webjing.issues.extension.IssueMessage;
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
public class ListedIssueMessage {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueMessage issueMessage;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVo contributorVo;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Stats stats;
}
