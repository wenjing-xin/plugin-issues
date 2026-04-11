package com.webjing.issues.service;

import com.webjing.issues.entity.ListedIssueComment;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.query.IssueCommentQuery;
import com.webjing.issues.vo.IssueCommentVO;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * issue 评论的相关业务接口
 * @author: webjing
 * @date: 2025年05月26日 09:25
 * @description:
 */
public interface IssueCommentService {

    Mono<IssueComment> create(IssueComment issueComment);

    Mono<ListResult<ListedIssueComment>> listIssueComment(IssueCommentQuery query);

    Mono<IssueComment> updateBy(IssueComment issueComment);

    Mono<IssueComment> deleteBy(IssueComment issueComment);

    Mono<IssueComment> getByUsername(String issueCommentName, String name);

    Mono<IssueComment.IssueCommentContent> getIssueCommentContent(String issueCommentName);
}
