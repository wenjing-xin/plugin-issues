package com.webjing.issues.vo;

import lombok.Data;
import run.halo.app.extension.ListResult;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年04月02日 14:42
 */
@Data
public class ListedIssueOverview {

    private IssueMessageVo issueMessageVo;

    private ListResult<IssueMessageVo> issueMessageVos;

}
