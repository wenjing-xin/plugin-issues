package com.webjing.issues.service;

import com.webjing.issues.entity.IssueTemplateOptions;
import com.webjing.issues.entity.IssueTemplateRender;
import com.webjing.issues.extension.IssueSubject;
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

    /**
     * 根据主题类型过滤 Issue应有的模版
     * @param subjectTypeName
     * @return
     */
    Mono<IssueTemplateOptions> listIssueTemplateOptions(String subjectTypeName, String subjectName);

    /**
     * 构建 Issue 模版数据
     * @param templateName
     * @return
     */
    Mono<IssueTemplateRender> buildTemplateData(String templateName);


}
