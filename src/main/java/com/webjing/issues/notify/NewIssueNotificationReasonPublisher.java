package com.webjing.issues.notify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.webjing.issues.Constant;
import com.webjing.issues.event.IssueCreatedEvent;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;
import java.util.List;
import java.util.Map;
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
                List<String> participateUsers = issueSubject.getSpec().getParticipateUsers();
                participateUsers.add(issueSubject.getSpec().getOwner());
                String issueSubjectTypeName = switch (issueSubject.getSpec().getSubjectType()) {
                    case POST -> "文章";
                    case PROJECT -> "项目";
                    case PRODUCT -> "产品";
                    case TOPIC -> "话题";
                    case LEAVE_MESSAGE -> "留言";
                };
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
            String owner = issue.getSpec().getOwner();
            notificationReasonEmitter.emit(Constant.HAS_NEW_ISSUE_ON_SUBJECT,
                builder -> {
                    var attributes = IssueCreatedReasonData.builder()
                        .issueTitle(issue.getSpec().getTitle())
                        .issueStatus(issue.getStatus().getState().name())
                        .issueCreatedAt(issue.getMetadata().getCreationTimestamp().toString())
                        .issueRawContent(issue.getSpec().getContent().getRaw())
                        .issueHtmlContent(issue.getSpec().getContent().getHtml())
                        .issuePermalink(issue.getStatus().getPermalink())
                        .issueOwner(issue.getSpec().getOwner())
                        .receiveOwner(participateUser)
                        .approved(issue.getSpec().getApproved())
                        .subjectDisplayName(subjectDisplayName)
                        .subjectType(subjectType)
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.of(owner))
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

    @UtilityClass
    static class ReasonDataConverter {
        public static <T> Map<String, Object> toAttributeMap(T data) {
            Assert.notNull(data, "Reason attributes must not be null");
            return JsonUtils.mapper().convertValue(data, new TypeReference<>() {
            });
        }
    }


}
