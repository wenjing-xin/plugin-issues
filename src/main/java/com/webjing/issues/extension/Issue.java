package com.webjing.issues.extension;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.webjing.issues.Constant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
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
    kind = Issue.KIND, plural = "issues", singular = "issue")
public class Issue extends AbstractExtension {

    public static final String KIND = "Issue";

    public static final String REQUIRE_SYNC_ON_STARTUP_INDEX_NAME = "requireSyncOnStartup";

    @Schema(requiredMode = REQUIRED)
    private IssueSpec spec;

    @Schema(requiredMode = REQUIRED)
    private IssueStatus status;

    @Data
    public static class IssueSpec {

        @Schema(requiredMode = REQUIRED)
        private String title;

        @Schema(requiredMode = REQUIRED, description = "Owner of the issue message")
        private String owner;

        @Schema(requiredMode = REQUIRED)
        private IssueContent content;

        private Set<String> assignees; // 经办人列表

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

        @Schema(description = "关联主体唯一标识", requiredMode = REQUIRED)
        private String subjectName;

        @Schema(requiredMode = REQUIRED, defaultValue = "false", description = "是否置顶")
        private Boolean top;
    }

    @Data
    public static class IssueStatus {

        @Schema(description = "当前状态", requiredMode = REQUIRED)
        private IssueState state =  IssueState.AWAIT;

        private String permalink;

        private long observedVersion;

        @Schema(description = "状态变更历史（时间+操作人）")
        private List<StateTransition> transitions = new ArrayList<>();

        @Schema(description = "优先级（P0-P3）", defaultValue = "P2")
        private String priority;
    }

    @Data
    public static class StateTransition {
        private Instant time;
        private String operator;
        private IssueState fromState;
        private IssueState toState;
        private String comment;
    }

    @Data
    public static class IssueContent {

        @Schema(description = "Raw of content", requiredMode = REQUIRED)
        private String raw;

        @Schema(description = "Rendered result with HTML format", requiredMode = REQUIRED)
        private String html;

        @Schema(description = "rendered medium")
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

    public static String parseIssueState(IssueState type) {
        return switch (type) {
            case AWAIT -> "待处理";
            case PROGRESS -> "进行中";
            case CLOSED -> "已关闭";
        };
    }

}
