package com.webjing.issues.entity;

import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.vo.ContributorVO;
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
    private ContributorVO contributorVo;

    private String subjectDisplayName;

}
