package com.webjing.issues.service;

import com.webjing.issues.entity.IssueTemplateOptions;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.query.IssueQuery;
import com.webjing.issues.entity.ListedIssue;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 相关的接口
 * @author: webjing
 * @date: 2025年03月06日 14:54
 */
public interface IssueService {

    Mono<ListResult<ListedIssue>> listIssue(IssueQuery query);

    Mono<Issue> create(Issue issue);

    Flux<String> listAllLabels(IssueQuery query);

    Mono<ListedIssue> findIssueByName(String name);

    Mono<Issue> getByUsername(String issueName, String username);

    Mono<Issue> updateBy(Issue issue);

    Mono<Issue> deleteBy(Issue issue);

    Mono<Issue> closeIssue(Issue issue,  String closedComment, String closedOwner);

    Mono<Issue.IssueContent> getIssueContent(String issueName);

    Mono<Issue> reopenIssue(Issue issue, String reopenOwner);

    Mono<Issue> setAwaitIssue(Issue issue, String setAwaitOwner);

    Mono<Issue> consoleUpdateIssue(Issue issue);

    Mono<IssueTemplateOptions> listIssueSelectTemplateOptions(String subjectName);
}
