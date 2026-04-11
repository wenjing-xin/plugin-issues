package com.webjing.issues.reconciler;

import static run.halo.app.extension.ExtensionUtil.addFinalizers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.webjing.issues.event.IssueCreatedEvent;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import com.webjing.issues.search.IssueDocumentConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.search.event.HaloDocumentAddRequestEvent;
import run.halo.app.search.event.HaloDocumentDeleteRequestEvent;

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

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    private final IssueDocumentConverter converter;

    @Override
    public Result reconcile(Request request) {
        client.fetch(Issue.class, request.name()).ifPresent(issue -> {
            if (ExtensionUtil.isDeleted(issue)) {
                if (ExtensionUtil.removeFinalizers(issue.getMetadata(), Set.of(FINALIZER))) {
                    eventPublisher.publishEvent(
                        new HaloDocumentDeleteRequestEvent(this,
                            List.of(converter.haloDocId(issue)))
                    );
                    client.update(issue);
                }
                return;
            }

            if (addFinalizers(issue.getMetadata(), Set.of(FINALIZER))) {
                notificationSubscriptionHelper.subscribeNewCommentReasonForIssue(issue);
                client.update(issue);
                eventPublisher.publishEvent(new IssueCreatedEvent(this, issue.getMetadata().getName()));
            }
            var haloDoc = converter.convert(issue).blockOptional().orElseThrow();
            eventPublisher.publishEvent(new HaloDocumentAddRequestEvent(this, List.of(haloDoc)));

            var status = issue.getStatus();
            if (status == null) {
                status = new Issue.IssueStatus();
                issue.setStatus(status);
            }
            status.setObservedVersion(issue.getMetadata().getVersion() + 1);

            // add approved marks to the old data by default.
            if (issue.getSpec().getApproved() == null) {
                issue.getSpec().setApproved(true);
            }
            if (issue.getSpec().getApproved() && issue.getSpec().getApprovedTime() == null) {
                issue.getSpec().setApprovedTime(Instant.now());
            }
            if(issue.getSpec().getApproved() && issue.getSpec().getReleaseTime() != null){
                //设置发布的issue链接
                issue.getStatus().setPermalink("/subject/" + issue.getSpec().getSubjectName() + "/issues/" + issue.getMetadata().getName());
            }
            client.update(issue);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        Issue issue = new Issue();
        return builder
            .extension(issue)
            .workerCount(5)
            .build();
    }
}
