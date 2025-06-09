package com.webjing.issues.notify;

import com.webjing.issues.Constant;
import com.webjing.issues.event.IssueClosedEvent;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.util.ReasonDataConverterUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;
import java.util.Objects;
import java.util.Set;

import static run.halo.app.extension.MetadataUtil.nullSafeAnnotations;

/**
 * @description: Issue 关闭事件
 * @className: ClosedIssueNotificationReasonPublisher
 * @author: webjing
 * @date: 2025年05月30日 11:02
 */
@Component
@RequiredArgsConstructor
public class ClosedIssueNotificationReasonPublisher {

    private final ExtensionClient client;

    private final ClosedIssueReasonPublisher closedIssueReasonPublisher;

    /**
     * On closing issue.
     */
    @Async
    @EventListener(IssueClosedEvent.class)
    public void onNewIssue(IssueClosedEvent event) {
        Issue issue = client.fetch(Issue.class, event.getIssueName()).get();
        var annotations = nullSafeAnnotations(issue);
        var newIssueNotified = annotations.getOrDefault(Constant.CLOSED_ISSUE_NOTIFIED_ANNO,"false");
        if (Objects.equals(newIssueNotified,"false")) {
            client.fetch(IssueSubject.class, issue.getSpec().getSubjectName()).map(issueSubject -> {
                Set<String> watchers = issue.getSpec().getAssignees();
                watchers.add(issue.getSpec().getOwner());
                String issueSubjectTypeName = IssueSubject.parseSubjectType(issueSubject.getSpec().getSubjectType());
                watchers.forEach(
                    participateUser -> closedIssueReasonPublisher.publishReasonBy(issue,
                        participateUser, issueSubject.getSpec().getDisplayName(), issueSubjectTypeName, event.getClosedComment(), event.getClosedOwner()));
                return Mono.empty();
            });
            //添加已经通知的标识
            annotations.put(Constant.CLOSED_ISSUE_NOTIFIED_ANNO, "true");
            client.update(issue);
        }
    }

    @Component
    @RequiredArgsConstructor
    static class ClosedIssueReasonPublisher {

        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;

        public void publishReasonBy(Issue issue, String participateUser, String subjectDisplayName, String subjectType, String closedComment, String closedOwner) {
            Boolean approved = issue.getSpec().getApproved();
            String contentUrl;
            if(approved){
                contentUrl = externalLinkProcessor.processLink(issue.getStatus().getPermalink());
            }else{
                contentUrl = externalLinkProcessor.processLink("/console/issueSubject/issues?subjectName=" + issue.getSpec().getSubjectName() + "&approved=false");
            }
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(issue.getApiVersion())
                .kind(issue.getKind())
                .name(issue.getMetadata().getName())
                .title(issue.getSpec().getTitle())
                .url(contentUrl)
                .build();
            String owner = issue.getSpec().getOwner();
            notificationReasonEmitter.emit(Constant.MANAGER_CLOSED_ISSUE,
                builder -> {
                    var attributes = IssueClosedReasonData.builder()
                        .issueTitle(issue.getSpec().getTitle())
                        .issueClosedTime(issue.getSpec().getClosedAt().toString())
                        .closedComment(closedComment)
                        .issuePermalink(contentUrl)
                        .closedOwner(closedOwner)
                        .receiveOwner(participateUser)
                        .subjectDisplayName(subjectDisplayName)
                        .subjectType(subjectType)
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(owner))
                        .subject(reasonSubject);
                }).block();

        }

        @Builder
        record IssueClosedReasonData(String issueTitle, String issueClosedTime, String closedComment, String issuePermalink, String issueOwner,String closedOwner,
                                      String receiveOwner, String subjectDisplayName, String subjectType) {
        }

    }
}
