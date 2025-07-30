package com.webjing.issues.service;

import lombok.Data;
import reactor.core.publisher.Mono;

public interface SettingConfigGetter {

    Mono<IssuesBasic> getIssuesBasic();

    @Data
    class IssuesBasic {

        public static final String GROUP = "issuesBasic";

        private String contentStyle;

        private int pageSize;

        private String defaultClosedComment;

        private String defaultAvatarMode;

        private String diceBarAvatarStyle;

        private Integer diceBarAvatarSize;

    }

}
