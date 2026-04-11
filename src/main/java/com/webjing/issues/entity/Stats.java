package com.webjing.issues.entity;

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

    private Integer upvote;

    private Integer downvote;

    public static Stats empty() {
        return Stats.builder()
            .upvote(0)
            .downvote(0)
            .build();
    }
}
