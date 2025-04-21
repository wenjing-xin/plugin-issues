package com.webjing.issues.vo;

import lombok.Builder;
import lombok.Value;

/**
 * 统计数据
 * @author: webjing
 * @date: 2025年03月10日 11:33
 */
@Value
@Builder
public class Stats {

    Integer upvote;

    Integer totalComment;

    Integer approvedComment;

    public static Stats empty() {
        return Stats.builder().upvote(0)
            .totalComment(0)
            .approvedComment(0)
            .build();
    }
}
