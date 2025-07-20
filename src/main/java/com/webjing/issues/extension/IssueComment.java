package com.webjing.issues.extension;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * issue 评论回复
 * @author: webjing
 * @date: 2025年03月17日 10:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = Constant.GROUP, version = Constant.VERSION,
    kind = IssueComment.KIND, singular = "issuecomment", plural = "issuecomments")
public class IssueComment extends AbstractExtension {

    public static final String KIND = "IssueComment";

    @Schema(requiredMode = REQUIRED)
    private IssueCommentSpec spec;

    @Data
    public static class IssueCommentSpec {

        @Schema(requiredMode = REQUIRED)
        private String issueName;

        @Schema(description = "父评论 UID（为空表示顶层评论）")
        private String quoteCommentUid;

        private String userAgent;

        private String ipAddress;

        @Schema(requiredMode = REQUIRED, description = "Owner of the issue message")
        private String owner;

        @Schema(requiredMode = REQUIRED)
        private IssueCommentContent content;

        @Schema(defaultValue = "false")
        private Boolean approved;

        @Schema(description = "approvedTime of the issue message")
        private Instant approvedTime;

        @Schema(defaultValue = "true", description = "是否允许通知")
        private Boolean allowNotification;

        @Schema(requiredMode = REQUIRED, defaultValue = "false", description = "是否置顶")
        private Boolean top;

        @Schema(requiredMode = REQUIRED, defaultValue = "false", description = "是否隐藏")
        private Boolean hidden;

    }

    @Data
    public static class IssueCommentContent {

        @Schema(description = "Raw of content")
        private String raw;

        @Schema(description = "Rendered result with HTML format")
        private String html;

        @ArraySchema(
            uniqueItems = true,
            arraySchema = @Schema(description = "Medium of issueDetail"),
            schema = @Schema(description = "Media item of issueDetail"))
        private List<IssueCommentMedia> medium;
    }

    @Data
    public static class IssueCommentMedia {

        @Schema(description = "Type of media")
        private IssueDetailMediaType type;

        @Schema(description = "External URL of media")
        private String url;

        @Schema(description = "Origin type of media.")
        private String originType;
    }

    public enum IssueDetailMediaType {
        PHOTO,
        POST,
    }
}
