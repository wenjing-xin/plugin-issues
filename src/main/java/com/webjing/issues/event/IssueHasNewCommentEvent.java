package com.webjing.issues.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.halo.app.core.extension.content.Comment;

/**
 * 当有新评论的时候的触发事件
 * @author: webjing
 * @date: 2025年03月07日 20:43
 */
@Getter
public class IssueHasNewCommentEvent extends ApplicationEvent {

    private final Comment comment;

    public IssueHasNewCommentEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }

}
