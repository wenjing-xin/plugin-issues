package com.webjing.issues.vo;

import lombok.Builder;
import lombok.Value;

/**
 *
 * @author: webjing
 * @date: 2025年03月10日 14:51
 */
@Value
@Builder
public class IssueLabelVO {

    String name;

    String permalink;

    Integer momentCount;

}
