package com.webjing.issues.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * issue 删除事件
 *
 * @author: webjing
 * @date: 2025年03月07日 20:41
 */
@Getter
public class IssueMessageDeletedEvent extends ApplicationEvent {

    private final String issueName;

    public IssueMessageDeletedEvent(Object source, String issueName) {
        super(source);
        this.issueName = issueName;
    }
}
