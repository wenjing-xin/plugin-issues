package com.webjing.issues.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @description: issue评论创建事件
 * @className: IssueCommentCreatedEvent
 * @author: webjing
 * @date: 2025年07月18日 00:18
 */
@Getter
public class IssueCommentCreatedEvent extends ApplicationEvent {

    private final String issueCommentName;

    public IssueCommentCreatedEvent(Object source, String issueCommentName) {
        super(source);
        this.issueCommentName = issueCommentName;
    }

}
