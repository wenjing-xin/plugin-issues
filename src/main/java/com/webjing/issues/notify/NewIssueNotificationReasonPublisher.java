package com.webjing.issues.notify;

import com.webjing.issues.Constant;
import com.webjing.issues.event.IssueCreatedEvent;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static run.halo.app.extension.MetadataUtil.nullSafeAnnotations;

/**
 * @description: 创建issue 通知原因
 * @className: IssueNotificationReasonPublisher
 * @author: webjing
 * @date: 2025年05月27日 11:20
 */
@Component
@RequiredArgsConstructor
public class NewIssueNotificationReasonPublisher {

    private final ExtensionClient client;

    private final NewIssueOnSubjectReasonPublisher newIssueOnSubjectReasonPublisher;

    /**
     * On new issue.
     */
    @Async
    @EventListener(IssueCreatedEvent.class)
    public void onNewIssue(IssueCreatedEvent event) {
        String issueName = event.getIssueName();
        Issue issue = client.fetch(Issue.class, issueName).get();
        var annotations = nullSafeAnnotations(issue);
        var newIssueNotified = annotations.getOrDefault(Constant.NEW_ISSUE_NOTIFIED_ANNO,"false");
        //只针对没有通知的issue进行通知
        if (Objects.equals(newIssueNotified,"false")) {
            client.fetch(IssueSubject.class, issue.getSpec().getSubjectName()).map(issueSubject -> {
                List<String> participateUsers = new ArrayList<>(issueSubject.getSpec().getParticipateUsers());
                participateUsers.add(issueSubject.getSpec().getOwner());
                String issueSubjectTypeName = IssueSubject.parseSubjectType(issueSubject.getSpec()
                    .getSubjectType());
                participateUsers.forEach(
                    participateUser -> newIssueOnSubjectReasonPublisher.publishReasonBy(issue,
                        participateUser, issueSubject.getSpec().getDisplayName(), issueSubjectTypeName));
                return Mono.empty();
            });
            //添加已经通知的标识
            annotations.put(Constant.NEW_ISSUE_NOTIFIED_ANNO, "true");
            client.update(issue);
        }
    }

    @Component
    @RequiredArgsConstructor
    static class NewIssueOnSubjectReasonPublisher {

        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;

        public void publishReasonBy(Issue issue, String participateUser, String subjectDisplayName, String subjectType) {
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
            notificationReasonEmitter.emit(Constant.HAS_NEW_ISSUE_ON_SUBJECT,
                builder -> {
                    var attributes = IssueCreatedReasonData.builder()
                        .issueTitle(issue.getSpec().getTitle())
                        .issueStatus(Issue.parseIssueState(issue.getStatus().getState()))
                        .issueCreatedAt(issue.getMetadata().getCreationTimestamp()
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                        .issueRawContent(issue.getSpec().getContent().getRaw())
                        .issueHtmlContent(issue.getSpec().getContent().getHtml())
                        .issuePermalink(contentUrl)
                        .issueOwner(issue.getSpec().getOwner())
                        .receiveOwner(participateUser)
                        .approved(issue.getSpec().getApproved())
                        .subjectDisplayName(subjectDisplayName)
                        .subjectType(subjectType)
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(issue.getSpec().getOwner()))
                        .subject(reasonSubject);
                }).block();

        }

        @Builder
        record IssueCreatedReasonData(String issueTitle, String issueStatus, String issueCreatedAt,
                                       String issueRawContent, String issueHtmlContent, String issuePermalink,
                                       String issueOwner, String receiveOwner, boolean approved,
                                        String subjectDisplayName, String subjectType) {
        }

    }

}
