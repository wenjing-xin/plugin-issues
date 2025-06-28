package com.webjing.issues.service;

import com.webjing.issues.entity.IssueLabelOptions;
import com.webjing.issues.entity.ListedIssueLabel;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.query.IssueLabelQuery;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * @author: webjing
 * @date: 2025年06月26日 09:53
 * @description:
 */
public interface IssueLabelService {

    Mono<ListResult<ListedIssueLabel>> listIssueLabels(IssueLabelQuery query);

    Mono<IssueLabel> create(IssueLabel issueLabel);

    Mono<IssueLabelOptions> listSubjectIssueLabels(String subjectName, String keyword);
}
