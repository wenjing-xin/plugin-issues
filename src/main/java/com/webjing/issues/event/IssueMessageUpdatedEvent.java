package com.webjing.issues.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * issue 更新事件
 * @author: webjing
 * @date: 2025年03月07日 20:42
 */
@Getter
public class IssueMessageUpdatedEvent extends ApplicationEvent {

    private final String issueName;

    public IssueMessageUpdatedEvent(Object source, String issueName) {
        super(source);
        this.issueName = issueName;
    }

}
