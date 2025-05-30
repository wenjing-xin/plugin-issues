package com.webjing.issues.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * issue 关闭事件
 * @author: webjing
 * @date: 2025年03月07日 20:42
 */
@Getter
public class IssueClosedEvent extends ApplicationEvent {

    private final String issueName;

    public IssueClosedEvent(Object source, String issueName) {
        super(source);
        this.issueName = issueName;
    }

}
