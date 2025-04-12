package com.webjing.issues.extension;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * issue 类型的消息数据模型
 * @author: webjing
 * @date: 2025年02月28日 08:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = Constant.GROUP, version = Constant.VERSION,
    kind = IssueMessage.KIND, plural = "issuemessages", singular = "issuemessage")
public class IssueMessage extends AbstractExtension {

    public static final String KIND = "IssueMessage";

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    @Schema(requiredMode = REQUIRED)
    private IssueMessageSpec spec;

    private IssueMessageStatus status;

    @Data
    public static class IssueMessageSpec {

        @Schema(requiredMode = REQUIRED)
        private String title;

        @Schema(requiredMode = REQUIRED, description = "Owner of the issue message")
        private String owner;

        @Schema(requiredMode = REQUIRED)
        private IssueContent content;

        private List<String> assignees; // 经办人列表

        private Set<String> labels; // 标签名称集合

        private String issueTemplate; // issue类型

        @Schema(defaultValue = "false")
        private Boolean approved;

        @Schema(description = "approvedTime of the issue message")
        private Instant approvedTime;

        @Schema(description = "closed time of the issue message")
        private Instant closedAt;

        @Schema(description = "Release timestamp. This field can be customized by owner")
        private Instant releaseTime;
    }

    @Data
    public static class IssueMessageStatus {
        private IssueState state =  IssueState.AWAIT;
        private Integer replayCount; // 动态计算的回复数
        private String closeReason; // CLOSED 状态的关闭原因
        private String permalink;
        private long observedVersion;
    }

    @Data
    public static class IssueContent {

        @Schema(description = "Raw of content")
        private String raw;

        @Schema(description = "Rendered result with HTML format")
        private String html;

        @ArraySchema(
            uniqueItems = true,
            arraySchema = @Schema(description = "Medium of issueMessage"),
            schema = @Schema(description = "Media item of issueMessage"))
        private List<IssueMedia> medium;
    }

    @Data
    public static class IssueMedia {

        @Schema(description = "Type of media")
        private IssueMediaType type;

        @Schema(description = "External URL of media")
        private String url;

        @Schema(description = "Origin type of media.")
        private String originType;
    }

    // 状态枚举
    public enum IssueState {
        AWAIT, // 待处理`
        PROGRESS, // 进行中
        CLOSED, // 关闭
    }

    public enum IssueMediaType {
        PHOTO,
        POST,
    }

}
