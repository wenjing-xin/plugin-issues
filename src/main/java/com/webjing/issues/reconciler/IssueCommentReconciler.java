package com.webjing.issues.reconciler;

import com.webjing.issues.event.IssueCreatedEvent;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.extension.DefaultExtensionMatcher;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;
import java.time.Instant;
import java.util.Set;

import static run.halo.app.extension.ExtensionUtil.addFinalizers;
import static run.halo.app.extension.index.query.QueryFactory.equal;

/**
 * @description:
 * @className: IssueCommentReconciler
 * @author: webjing
 * @date: 2025年07月20日 14:33
 */
@Component
@RequiredArgsConstructor
public class IssueCommentReconciler  implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "issue-message-protection";

    private final ExtensionClient client;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Result reconcile(Request request) {
        client.fetch(IssueComment.class, request.name()).ifPresent( issueComment -> {
            if (ExtensionUtil.isDeleted(issueComment)) {
                if (ExtensionUtil.removeFinalizers(issueComment.getMetadata(), Set.of(FINALIZER))) {
                    client.update(issueComment);
                }
                return;
            }

            if (addFinalizers(issueComment.getMetadata(), Set.of(FINALIZER))) {
                if(StringUtils.isNotBlank(issueComment.getSpec().getQuoteCommentUid())){
                    Issue issue = client.fetch(Issue.class, issueComment.getSpec().getIssueName()).get();
                    notificationSubscriptionHelper.subscribeNewReplyCommentReasonForIssueComment(issue, issueComment);
                    client.update(issueComment);
                }
                // eventPublisher.publishEvent(new IssueCreatedEvent(this, issueComment.getMetadata().getName()));
            }

            // add approved marks to the old data by default.
            if (issueComment.getSpec().getApproved() == null) {
                issueComment.getSpec().setApproved(true);
            }
            if (issueComment.getSpec().getApproved() && issueComment.getSpec().getApprovedTime() == null) {
                issueComment.getSpec().setApprovedTime(Instant.now());
            }

            client.update(issueComment);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        IssueComment issueComment = new IssueComment();
        return builder
            .extension(issueComment)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, issueComment.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(IssueComment.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }

}
