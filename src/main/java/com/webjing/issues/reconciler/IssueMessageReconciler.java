package com.webjing.issues.reconciler;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.time.Instant;
import java.util.Set;
import com.webjing.issues.event.IssueMessageDeletedEvent;
import com.webjing.issues.event.IssueMessageUpdatedEvent;
import com.webjing.issues.extension.IssueMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;
import run.halo.app.notification.NotificationCenter;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月07日 20:39
 */
@Component
@RequiredArgsConstructor
public class IssueMessageReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "issue-message-protection";
    private final ExtensionClient client;
    private final NotificationCenter notificationCenter;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Result reconcile(Request request) {
        client.fetch(IssueMessage.class, request.name()).ifPresent(issueMessage -> {
            if (ExtensionUtil.isDeleted(issueMessage)) {
                if (ExtensionUtil.removeFinalizers(issueMessage.getMetadata(), Set.of(FINALIZER))) {
                    client.update(issueMessage);
                    eventPublisher.publishEvent(new IssueMessageDeletedEvent(this, request.name()));
                }
                return;
            }
            if (ExtensionUtil.addFinalizers(issueMessage.getMetadata(), Set.of(FINALIZER))) {
                // auto subscribe to new comment on issueMessage
                createCommentSubscriptionForIssueMessage(issueMessage);
            }
            var status = issueMessage.getStatus();
            if (status == null) {
                status = new IssueMessage.IssueMessageStatus();
                issueMessage.setStatus(status);
            }
            status.setObservedVersion(issueMessage.getMetadata().getVersion() + 1);
            // add approved marks to the old data by default.
            if (issueMessage.getSpec().getApproved() == null) {
                issueMessage.getSpec().setApproved(true);
            }
            if (issueMessage.getSpec().getApproved() && issueMessage.getSpec().getApprovedTime() == null) {
                issueMessage.getSpec().setApprovedTime(Instant.now());
                // set permalink
                String permalink = "/issues/" + issueMessage.getMetadata().getName();
                issueMessage.getStatus().setPermalink(permalink);
            }
            client.update(issueMessage);

            eventPublisher.publishEvent(new IssueMessageUpdatedEvent(this, request.name()));
        });
        return Result.doNotRetry();
    }

    void createCommentSubscriptionForIssueMessage(IssueMessage issueMessage) {
        var owner = issueMessage.getSpec().getOwner();
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType("new-comment-on-issueMessage");
        interestReason.setExpression("props.issueMessageOwner == '%s'".formatted(owner));
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(owner);
        notificationCenter.subscribe(subscriber, interestReason).block();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        final var issueMessage = new IssueMessage();
        return builder
            .extension(issueMessage)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, issueMessage.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(IssueMessage.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }
}
