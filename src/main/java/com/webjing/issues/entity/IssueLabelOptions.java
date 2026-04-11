package com.webjing.issues.entity;

import com.webjing.issues.extension.IssueLabel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * @description:
 * @className: IssueLabelOptions
 * @author: webjing
 * @date: 2025年06月27日 15:15
 */
@Data
public class IssueLabelOptions {

    private List<IssueLabelItem> issueLabelOptions;

    @Data
    @Builder
    public static class IssueLabelItem{

        private String label;

        private String value;

        public static IssueLabelItem from(IssueLabel issueLabel) {
            return IssueLabelItem.builder()
                .label(issueLabel.getSpec().getLabelName())
                .value(issueLabel.getMetadata().getName())
                .build();
        }
    }

}
