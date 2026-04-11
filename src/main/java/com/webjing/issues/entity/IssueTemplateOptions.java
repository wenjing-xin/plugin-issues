package com.webjing.issues.entity;

import com.webjing.issues.extension.IssueTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @description:  Issue模版选项构建数据模型
 * @className: IssueTemplateOptions
 * @author: webjing
 * @date: 2025年07月13日 09:50
 */
@Data
public class IssueTemplateOptions {

    @Schema(description = "issue模版选项", requiredMode = REQUIRED)
    private List<IssueTemplateItem> issueTemplateOptions = new ArrayList<>();

    @Data
    @Builder
    public static class IssueTemplateItem{

        private String label;

        private String value;

        public static IssueTemplateItem from(IssueTemplate issueTemplate) {
            return IssueTemplateItem.builder()
                .label(issueTemplate.getSpec().getName())
                .value(issueTemplate.getMetadata().getName())
                .build();
        }
    }

}
