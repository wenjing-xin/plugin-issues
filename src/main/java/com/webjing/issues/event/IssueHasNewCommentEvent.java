package com.webjing.issues.event;

import com.webjing.issues.extension.IssueComment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.halo.app.core.extension.content.Comment;

/**
 * 当有新回复的时候的触发事件
 * @author: webjing
 * @date: 2025年03月07日 20:43
 */
@Getter
public class IssueHasNewCommentEvent extends ApplicationEvent {

    private final IssueComment issueComment;

    public IssueHasNewCommentEvent(Object source, IssueComment issueComment) {
        super(source);
        this.issueComment = issueComment;
    }

}
