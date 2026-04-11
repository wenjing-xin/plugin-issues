package com.webjing.issues.service;

import com.webjing.issues.entity.IssueSubjectStats;
import com.webjing.issues.entity.ListedIssueSubject;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.query.IssueSubjectQuery;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 主体相关接口
 * @author: webjing
 * @date: 2025年05月03日 18:18
 */
public interface IssueSubjectService {

    Mono<IssueSubject> create(IssueSubject issueSubject);

    Mono<ListResult<ListedIssueSubject>> listIssueSubject(IssueSubjectQuery query);

    Mono<IssueSubjectStats> fetchIssueSubjectStats(String issueSubjectName);

    Mono<IssueSubject> updateIssueSubject(IssueSubject issueSubject);
}
