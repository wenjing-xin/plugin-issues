package com.webjing.issues.vo;

import com.webjing.issues.extension.IssueTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月17日 11:50
 */
@Data
@Builder
public class ListedIssueTemplate {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueTemplate issueTemplate;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVo contributorVo;

}
