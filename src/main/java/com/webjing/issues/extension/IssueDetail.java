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
 * issue 详情回复
 * @author: webjing
 * @date: 2025年03月17日 10:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = Constant.GROUP, version = Constant.VERSION,
    kind = IssueDetail.KIND, singular = "issuedetail", plural = "issuedetails")
public class IssueDetail extends AbstractExtension {

    public static final String KIND = "IssueDetail";

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    @Schema(requiredMode = REQUIRED)
    private IssueDetailSpec spec;

    @Data
    public static class IssueDetailSpec {

        private String issueName;

        @Schema(requiredMode = REQUIRED)
        private String quote; // 引用内容

        @Schema(requiredMode = REQUIRED, description = "Owner of the issue message")
        private String owner;

        @Schema(requiredMode = REQUIRED)
        private IssueDetailContent content;

        @Schema(defaultValue = "false")
        private Boolean approved;

        @Schema(description = "approvedTime of the issue message")
        private Instant approvedTime;

        @Schema(defaultValue = "true", description = "是否允许通知")
        private Boolean allowNotification;

    }

    @Data
    public static class IssueDetailContent {

        @Schema(description = "Raw of content")
        private String raw;

        @Schema(description = "Rendered result with HTML format")
        private String html;

        @ArraySchema(
            uniqueItems = true,
            arraySchema = @Schema(description = "Medium of issueDetail"),
            schema = @Schema(description = "Media item of issueDetail"))
        private List<IssueDetailMedia> medium;
    }

    @Data
    public static class IssueDetailMedia {

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
