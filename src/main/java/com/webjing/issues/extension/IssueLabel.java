package com.webjing.issues.extension;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @description:
 * @className: IssueLabel
 * @author: webjing
 * @date: 2025年06月25日 11:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = Constant.GROUP, version = Constant.VERSION,
    kind = "IssueLabel", plural = "issuelabels", singular = "issuelabel")
public class IssueLabel extends AbstractExtension {

    @Schema(requiredMode = REQUIRED)
    private IssueLabel.IssueLabelSpec spec;

    @Data
    public static class IssueLabelSpec {

        @Schema(requiredMode = REQUIRED, description = "标签名称")
        private String labelName;

        @Schema(description = "标签描述")
        private String description;

        @Schema( description = "标签颜色")
        private String color;

        @Schema(description = "标签模版路径")
        private String slug;

        @Schema(requiredMode = REQUIRED, description = "标签生效范围", defaultValue = "false")
        private LabelScope scope;

        @Schema(description = "当标签范围为主体类型的时候，为必填项")
        private IssueSubject.SubjectType subjectType;

        @Schema(description = "标签范围为主体时的归属主体ID")
        private String subjectName;

    }

    public enum LabelScope {
        /**
         * 全局
         */
        GLOBAL,
        /**
         * 针对某一主体类型
         */
        SUBJECT_TYPE,
        /**
         * 某一主体
         */
        SUBJECT
    }

}
