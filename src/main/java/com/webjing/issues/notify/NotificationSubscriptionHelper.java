package com.webjing.issues.notify;

import com.webjing.issues.Constant;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.UserIdentity;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @className: NotificationSubscriptionHelper
 * @author: webjing
 * @date: 2025年05月27日 15:37
 */
@Component
@RequiredArgsConstructor
public class NotificationSubscriptionHelper {

    private final NotificationCenter notificationCenter;

    /**
     * Subscribe new issue reason for issueSubject.
     * @param issueSubject
     */
    public void subscribeNewIssueReasonForSubject(IssueSubject issueSubject) {
        // 当Issue依托主体有新的issue时，为创建者和所有参与者订阅通知
        List<String> participateUsers = issueSubject.getSpec().getParticipateUsers();
        // 为创建者订阅新 Issue 通知
        subscribeNewIssue(UserIdentity.of(issueSubject.getSpec().getOwner()));
        participateUsers.forEach(participateUser -> subscribeNewIssue(UserIdentity.of(participateUser)));
    }

    /**
     * 关闭 issue 的时候为issue拥有者和issue关注者进行通知
     * @param issue
     */
    public void subscribeClosedIssueReasonForSubject(Issue issue) {
        // 当issue被关闭的时候，为 issue 拥有者和关注者进行通知
        String issueOwner = issue.getSpec().getOwner();
        Set<String> watchers = issue.getSpec().getAssignees();
        // 为创建者订阅关闭 Issue 通知
        subscribeClosedIssueNotify(UserIdentity.of(issueOwner));
        watchers.forEach(participateUser -> subscribeClosedIssueNotify(UserIdentity.of(participateUser)));
    }

    /**
     * Subscribe new issueComment reason for issue.
     *
     * @param issue issue
     */
    public void subscribeNewCommentReasonForIssue(Issue issue) {
        var subjectOwner = issue.getSpec().getOwner();
        subscribeComment(UserIdentity.of(subjectOwner));
    }

    /**
     * Subscribe new issueComment reason for issue.
     *
     * @param issue issue
     */
    public void subscribeNewReplyCommentReasonForIssueComment(Issue issue) {
        var subjectOwner = issue.getSpec().getOwner();
        subscribeReplyComment(UserIdentity.of(subjectOwner));
    }

    /**
     * 为issue依托主体订阅新issue
     * @param identity
     */
    void subscribeNewIssue(UserIdentity identity) {
        var subscriber = createSubscriber(identity);
        if (subscriber == null) {
            return;
        }
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(Constant.HAS_NEW_ISSUE_ON_SUBJECT);
        interestReason.setExpression("props.receiveOwner == '%s'".formatted(identity.name()));
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    /**
     * 关闭issue的时候为相关用户订阅通知
     * @param identity
     */
    void subscribeClosedIssueNotify(UserIdentity identity) {
        var subscriber = createSubscriber(identity);
        if (subscriber == null) {
            return;
        }
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(Constant.MANAGER_CLOSED_ISSUE);
        interestReason.setExpression("props.receiveOwner == '%s'".formatted(identity.name()));
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    /**
     * 为issue订评论
     * @param identity
     */
    void subscribeComment(UserIdentity identity) {
        var subscriber = createSubscriber(identity);
        if (subscriber == null) {
            return;
        }
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(Constant.HAS_NEW_ISSUE_COMMENT);
        interestReason.setExpression("props.issueOwner == '%s'".formatted(identity.name()));
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    /**
     * 为issue评论订阅回复
     * @param identity
     */
    void subscribeReplyComment(UserIdentity identity){
        var subscriber = createSubscriber(identity);
        if (subscriber == null) {
            return;
        }
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(Constant.HAS_NEW_REPLY_ISSUE_COMMENT);
        interestReason.setExpression("props.receiveOwner == '%s'".formatted(identity.name()));
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    @Nullable
    private Subscription.Subscriber createSubscriber(UserIdentity author) {
        if (StringUtils.isBlank(author.name())) {
            return null;
        }

        Subscription.Subscriber subscriber = new Subscription.Subscriber();
        subscriber.setName(author.name());
        return subscriber;
    }



}
