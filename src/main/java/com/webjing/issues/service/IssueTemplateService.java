package com.webjing.issues.service;

import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.query.IssueTemplateQuery;
import com.webjing.issues.entity.ListedIssueTemplate;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 接口功能: issue留言模版接口
 * @author: webjing
 * @date: 2025年03月17日 11:39
 */
public interface IssueTemplateService {

    Mono<IssueTemplate> create(IssueTemplate issueTemplate);

    Mono<ListResult<ListedIssueTemplate>> listIssueTemplate(IssueTemplateQuery query);

}
