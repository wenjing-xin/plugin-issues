package com.webjing.issues.extension;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月17日 10:47
 */
@Data
@ToString(callSuper = true)
@GVK(kind = IssueTemplate.KIND, group = Constant.GROUP,
    version = "v1alpha1", singular = "issuetemplate", plural = "issuetemplates")
@EqualsAndHashCode(callSuper = true)
public class IssueTemplate extends AbstractExtension {

    public static final String KIND = "IssueTemplate";

    private IssueTemplateSpec spec;

    @Data
    public static class IssueTemplateSpec {

        @Schema(description = "IssueTemplate scope", requiredMode = REQUIRED)
        private IssueTemplateScope scope;

        private String subjectName;

        private IssueSubject.SubjectType subjectType;

        private String name;

        private String description;

        private Map<String, TemplateField> fields;

        private String owner;

    }

    @Data
    public static class TemplateField {

        @Schema(requiredMode = REQUIRED)
        private String key;

        @Schema(requiredMode = REQUIRED)
        private String title;

        private String defaultValue;

        private String placeholder;

        private String helpText;

        // 针对文本元素输入的最大长度
        private Integer minLength;

        private Integer maxLength;

        private Integer rows;

        // 正对选择器的options选项
        private List<Map<String, String>> fieldOptions;

        // 正则校验表达式
        private String validate;

        private String requiredMode;

        @Schema(requiredMode = REQUIRED, defaultValue = "TEXT")
        private TemplateFieldTypeEnum type;

    }

    public enum TemplateFieldTypeEnum{
        TEXT, // 纯文本
        TEXT_AREA, // 多行文本
        SELECT, // 下拉选择框
        RADIO,// radio 单选
        PASSWORD,//密码
        EMAIL, // 邮箱
    }

    public enum IssueTemplateScope{
        SUBJECT_TYPE, // 特定主体类型
        SUBJECT, // 特定主体
    }

}
