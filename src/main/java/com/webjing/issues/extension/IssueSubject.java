package com.webjing.issues.extension;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.util.List;
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

    @Schema(description = "依托对象详情", requiredMode = REQUIRED)
    private IssueSubjectSpec spec;

    @Data
    public static class IssueSubjectSpec {

        @Schema(description = "主体图标")
        private String subjectIcon;

        @Schema(description = "依托对象显示名称", requiredMode = REQUIRED)
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

        @Schema(description = "参与用户")
        private List<String> participateUsers;

        @Schema(description = "主体可见性", requiredMode = REQUIRED, defaultValue = "PUBLIC")
        private SubjectVisible subjectVisible;
    }

    /**
     * 依托内容
     */
    @Data
    public static class SubjectContent {

        @Schema(description = "依托内容UID，文章、产品等类型会存在一个ID")
        private String uid;

        @Schema(description = "依托的html内容", requiredMode = REQUIRED)
        private String htmlContent;

        @Schema(description = "依托的原内容", requiredMode = REQUIRED)
        private  String rawContent;

    }

    public enum SubjectVisible {
        /**
         * 公开
         */
        PUBLIC,
        /**
         * 私密
         */
        PRIVATE
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

    public static String parseSubjectType(SubjectType type) {
        return switch (type) {
            case POST -> "文章";
            case PROJECT -> "项目";
            case PRODUCT -> "产品";
            case TOPIC -> "话题";
            case LEAVE_MESSAGE -> "留言";
        };
    }

}
