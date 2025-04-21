package com.webjing.issues.finder;

import com.webjing.issues.vo.IssueMessageLabelVo;
import com.webjing.issues.vo.IssueMessageVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 14:39
 */
public interface IssueMessageFinder {
    /**
     * List all issues.
     *
     * @return a flux of issue vo.
     */
    Flux<IssueMessageVo> listAll();

    /**
     * List moments by page.
     *
     * @param page page number.
     * @param size page size.
     * @return a mono of list result.
     */
    Mono<ListResult<IssueMessageVo>> list(Integer page, Integer size);

    /**
     * List issues by label.
     *
     * @param label tag name.
     * @return a flux of issueMessage vo.
     */
    Flux<IssueMessageVo> listBy(String label);

    Mono<IssueMessageVo> get(String issueName);

    Flux<IssueMessageLabelVo> listAlllabels();

    Mono<ListResult<IssueMessageVo>> listByLabel(int pageNum, Integer pageSize, String labelName);
}
