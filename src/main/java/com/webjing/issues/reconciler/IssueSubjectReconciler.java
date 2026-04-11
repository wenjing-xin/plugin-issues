package com.webjing.issues.reconciler;

import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import java.util.Set;

import static run.halo.app.extension.ExtensionUtil.addFinalizers;

/**
 * @description: Issue依托主体调谐逻辑
 * @className: IssueSubjectReconciler
 * @author: webjing
 * @date: 2025年05月29日 09:09
 */
@Component
@RequiredArgsConstructor
public class IssueSubjectReconciler  implements Reconciler<Reconciler.Request> {

    private static final String FINALIZER = "issue-subject-protection";

    private final ExtensionClient client;

    private final NotificationSubscriptionHelper notificationSubscriptionHelper;

    @Override
    public Result reconcile(Request request) {
        client.fetch(IssueSubject.class, request.name()).ifPresent(issueSubject -> {
            if (ExtensionUtil.isDeleted(issueSubject)) {
                if (ExtensionUtil.removeFinalizers(issueSubject.getMetadata(), Set.of(FINALIZER))) {
                    client.update(issueSubject);
                }
                return;
            }
            if (addFinalizers(issueSubject.getMetadata(), Set.of(FINALIZER))) {
                // 为相应的参与用户订阅issue
                notificationSubscriptionHelper.subscribeNewIssueReasonForSubject(issueSubject);
            }
            client.update(issueSubject);
        });
        return Result.doNotRetry();
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        IssueSubject issueSubject = new IssueSubject();
        return builder.extension(issueSubject)
            .workerCount(5)
            .build();
    }

}
