package com.webjing.issues.service;


import com.webjing.issues.extension.IssueMessage;
import com.webjing.issues.query.IssueMessageQuery;
import com.webjing.issues.vo.ListedIssueMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 *
 * @author: webjing
 * @date: 2025年03月06日 14:54
 */
public interface IssueMessageService {

    Mono<ListResult<ListedIssueMessage>> listIssueMessage(IssueMessageQuery query);

    Mono<IssueMessage> create(IssueMessage issueMessage);

    Flux<String> listAllLabels(IssueMessageQuery query);

    Mono<ListedIssueMessage> findIssueMessageByName(String name);

    Mono<IssueMessage> getByUsername(String issueMessageName, String username);

    Mono<IssueMessage> updateBy(IssueMessage issueMessage);

    Mono<IssueMessage> deleteBy(IssueMessage issueMessage);
}
