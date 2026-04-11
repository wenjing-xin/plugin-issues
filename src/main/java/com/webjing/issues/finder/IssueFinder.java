package com.webjing.issues.finder;

import com.webjing.issues.vo.IssueCommentVO;
import com.webjing.issues.vo.IssueLabelVO;
import com.webjing.issues.vo.IssueVO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 14:39
 */
public interface IssueFinder {
    /**
     * List all issues.
     *
     * @return a flux of issue vo.
     */
    Flux<IssueVO> listAll();

    /**
     * List moments by page.
     *
     * @param page page number.
     * @param size page size.
     * @return a mono of list result.
     */
    Mono<ListResult<IssueVO>> list(Integer page, Integer size, String subjectName, String issueState);

    /**
     * List issues by label.
     *
     * @param label tag name.
     * @return a flux of issueMessage vo.
     */
    Flux<IssueVO> listBy(String label);

    Mono<IssueVO> get(String issueName);

    Flux<IssueLabelVO> listAlllabels();

    Mono<ListResult<IssueVO>> listByLabel(int pageNum, Integer pageSize, String labelName, String subjectName);

    /**
    * @Author webjing
    * @Description 列出所有的issue评论
    * @Date 15:10 2025/6/14
    * @Param [issueName]
    * @return reactor.core.publisher.Flux<com.webjing.issues.vo.IssueCommentVO>
    **/
    Flux<IssueCommentVO> listAllIssueComments(String issueName);

}
