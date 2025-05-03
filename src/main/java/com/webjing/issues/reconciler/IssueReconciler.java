package com.webjing.issues.reconciler;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.time.Instant;
import java.util.Set;
import com.webjing.issues.extension.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
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
public class IssueReconciler implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "issue-message-protection";

    private final ExtensionClient client;

    private final NotificationCenter notificationCenter;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Issue.class, request.name()).ifPresent(issueMessage -> {
            if (ExtensionUtil.isDeleted(issueMessage)) {
                if (ExtensionUtil.removeFinalizers(issueMessage.getMetadata(), Set.of(FINALIZER))) {
                    client.update(issueMessage);
                }
                return;
            }
            var status = issueMessage.getStatus();
            if (status == null) {
                status = new Issue.IssueMessageStatus();
                issueMessage.setStatus(status);
            }
            status.setObservedVersion(issueMessage.getMetadata().getVersion() + 1);
            // add approved marks to the old data by default.
            if (issueMessage.getSpec().getApproved() == null) {
                issueMessage.getSpec().setApproved(true);
            }
            if (issueMessage.getSpec().getApproved() && issueMessage.getSpec().getApprovedTime() == null) {
                issueMessage.getSpec().setApprovedTime(Instant.now());
            }
            if(issueMessage.getSpec().getApproved() && issueMessage.getSpec().getReleaseTime() != null){
                //设置发布的issue链接
                issueMessage.getStatus().setPermalink("/issues/" + issueMessage.getMetadata().getName());
            }
            client.update(issueMessage);
        });
        return Result.doNotRetry();
    }


    @Override
    public Controller setupWith(ControllerBuilder builder) {
        Issue issueMessage = new Issue();
        return builder
            .extension(issueMessage)
            .workerCount(5)
            .onAddMatcher(DefaultExtensionMatcher.builder(client, issueMessage.groupVersionKind())
                .fieldSelector(
                    FieldSelector.of(equal(Issue.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, "true"))
                )
                .build()
            )
            .build();
    }
}
