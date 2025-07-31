package com.webjing.issues.entity;

import com.webjing.issues.extension.Issue;
import lombok.Data;

/**
 * @description:
 * @className: IssueStatusChangeParam
 * @author: webjing
 * @date: 2025年07月31日 13:24
 */
@Data
public class IssueStatusChangeParam{

    private String changeComment;

    private String issueName;

    private Issue.IssueState issueState;
}
