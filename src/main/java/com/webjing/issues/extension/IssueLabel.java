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

        @Schema(requiredMode = REQUIRED, description = "是否全局标签", defaultValue = "false")
        private Boolean isGlobal;

        @Schema(description = "非全局标签的归属主体ID")
        private String subjectName;

    }

}
