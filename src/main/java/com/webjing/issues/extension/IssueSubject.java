package com.webjing.issues.extension;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.util.Set;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * issue 依托主体
 * @author: webjing
 * @date: 2025年04月22日 23:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = Constant.GROUP, version = Constant.VERSION,
    kind = IssueSubject.KIND, plural = "issuesubjects", singular = "issuesubject")
public class IssueSubject extends AbstractExtension {

    public static final String KIND = "IssueSubject";

    private IssueSubjectSpec spec;

    @Data
    public static class IssueSubjectSpec {

        @Schema(description = "依托对象显示名称")
        private String displayName;

        @Schema(description = "依托对象类型", requiredMode = REQUIRED)
        private SubjectType subjectType;

        @Schema(description = "依托内容", requiredMode = REQUIRED)
        private SubjectContent content;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "issue模版")
        private Set<String> issueTemplates;

        @Schema(description = "创建者", requiredMode = REQUIRED)
        private String owner;

    }

    /**
     * 依托内容
     */
    @Data
    public static class SubjectContent {

        @Schema(description = "依托内容UID，文章类型必须有")
        private String uid;

        private String htmlContent;

        private  String rawContent;

    }

    /**
     * 依托主体类型
     */
    public enum SubjectType {
        /**
         * halo 文章
         */
        POST,
        /**
         * 项目
         */
        PROJECT,
        /**
         * 产品
         */
        PRODUCT,
        /**
         * 话题
         */
        TOPIC,
        /**
         * 留言
         */
        LEAVE_MESSAGE
    }

}
