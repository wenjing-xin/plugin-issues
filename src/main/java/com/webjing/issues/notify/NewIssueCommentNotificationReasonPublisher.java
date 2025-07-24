package com.webjing.issues.notify;

import com.webjing.issues.Constant;
import com.webjing.issues.event.IssueCommentCreatedEvent;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.util.ReasonDataConverterUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
 * @description: 当 issue 下边有新的评论时候发送通知
 * @className: NewIssueCommentNotificationReasonPublisher
 * @author: webjing
 * @date: 2025年07月18日 00:08
 */
@Component
@RequiredArgsConstructor
public class NewIssueCommentNotificationReasonPublisher {

    private final ExtensionClient client;

    private final NewIssueCommentOnIssueReasonPublisher newIssueCommentOnIssueReasonPublisher;

    private final NewIssueReplyCommentOnIssueReasonPublisher newIssueReplyCommentOnIssueReasonPublisher;

    /**
     * On new issueComment.
     */
    @Async
    @EventListener(IssueCommentCreatedEvent.class)
    public void onNewIssueComment(IssueCommentCreatedEvent event) {
        String issueCommentName = event.getIssueCommentName();
        IssueComment issueComment = client.fetch(IssueComment.class, issueCommentName).get();
        var annotations = nullSafeAnnotations(issueComment);
        var newIssueNotified = annotations.getOrDefault(Constant.NEW_ISSUE_COMMENT_NOTIFIED_ANNO,"false");
        //只针对没有通知的issue进行通知
        if (Objects.equals(newIssueNotified,"false")) {
            client.fetch(Issue.class, issueComment.getSpec().getIssueName()).map(issue -> {
                List<String> needNotifyUsers = new ArrayList<>(issue.getSpec().getAssignees());

                if(StringUtils.isNotBlank(issueComment.getSpec().getQuoteCommentUid())){
                    // 引用的评论，调用回复通知
                    IssueComment originalIssueComment = client.fetch(IssueComment.class, issueComment.getSpec().getQuoteCommentUid()).get();
                    if(!issueComment.getSpec().getOwner().equals(originalIssueComment.getSpec().getOwner())){
                        // 引用评论的创建者不是评论者，需要通知引用评论的创建者
                        needNotifyUsers.add(originalIssueComment.getSpec().getOwner());
                    }
                    needNotifyUsers.forEach(
                        participateUser -> newIssueReplyCommentOnIssueReasonPublisher.publishReasonBy(issue, originalIssueComment, issueComment,
                            participateUser));
                }else{
                    if(!issue.getSpec().getOwner().equals(issueComment.getSpec().getOwner())){
                        needNotifyUsers.add(issue.getSpec().getOwner()); // 创建者在自己的issue下评论不会发送通知
                    }
                    needNotifyUsers.forEach(
                        participateUser -> newIssueCommentOnIssueReasonPublisher.publishReasonBy(issue, issueComment,
                            participateUser));
                }
                return Mono.empty();
            });
            //添加已经通知的标识
            annotations.put(Constant.NEW_ISSUE_COMMENT_NOTIFIED_ANNO, "true");
            client.update(issueComment);
        }
    }

    @Component
    @RequiredArgsConstructor
    static class NewIssueCommentOnIssueReasonPublisher {

        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;

        public void publishReasonBy(Issue issue, IssueComment issueComment, String needNotifyUser) {
            Boolean approved = issueComment.getSpec().getApproved();
            String contentUrl;
            String issueTitle;
            if(approved){
                contentUrl = externalLinkProcessor.processLink(issue.getStatus().getPermalink()) + "#" + issueComment.getMetadata().getName();
            }else{
                contentUrl = externalLinkProcessor.processLink("/console/issueSubject/issues?subjectName=" + issue.getSpec().getSubjectName() + "&approved=false");
            }

            if(issue.getSpec().getAssignees().contains(needNotifyUser)){
                issueTitle = "你负责经办的Issue【" + issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】下有新的评论";
            }else if (issue.getSpec().getOwner().equals(needNotifyUser)){
                issueTitle = "你创建的Issue【" + issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】下的有新的评论";
            }else{
                issueTitle = "你关注的Issue【" +  issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】有新的评论";
            }

            var reasonSubject = Reason.Subject.builder()
                .apiVersion(issueComment.getApiVersion())
                .kind(issueComment.getKind())
                .name(issueComment.getMetadata().getName())
                .title("Issue【" + issue.getSpec().getTitle() + "】下有了新的评论")
                .url(contentUrl)
                .build();
            notificationReasonEmitter.emit(Constant.HAS_NEW_ISSUE_COMMENT,
                builder -> {
                    var attributes = IssueCommentCreatedReasonData.builder()
                        .issueTitle(issueTitle)
                        .isApproved(issueComment.getSpec().getApproved())
                        .issueCommentCreatedAt(issueComment.getMetadata().getCreationTimestamp()
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                        .issueCommentRawContent(issueComment.getSpec().getContent().getRaw())
                        .issueCommentHtmlContent(issueComment.getSpec().getContent().getHtml())
                        .issueCommentPermalink(contentUrl)
                        .receiveOwner(needNotifyUser)
                        .issueCommentOwner(issueComment.getSpec().getOwner())
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(issueComment.getSpec().getOwner()))
                        .subject(reasonSubject);
                }).block();

        }

        @Builder
        record IssueCommentCreatedReasonData(String issueTitle, boolean isApproved, String issueCommentCreatedAt,
                                      String issueCommentRawContent, String issueCommentHtmlContent, String issueCommentPermalink,
                                      String receiveOwner, String issueCommentOwner) {
        }

    }

    @Component
    @RequiredArgsConstructor
    static class NewIssueReplyCommentOnIssueReasonPublisher {

        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;

        public void publishReasonBy(Issue issue, IssueComment originalIssueComment, IssueComment issueReplyComment, String needNotifyUser) {
            String contentUrl;
            String notifyTitle;
            if(issueReplyComment.getSpec().getApproved()){
                contentUrl = externalLinkProcessor.processLink(issue.getStatus().getPermalink()) + "#" + issueReplyComment.getMetadata().getName();
            }else{
                contentUrl = externalLinkProcessor.processLink("/console/issueSubject/issues?subjectName=" + issue.getSpec().getSubjectName() + "&approved=false");
            }

            if(needNotifyUser.equals(originalIssueComment.getSpec().getOwner())){
                notifyTitle =  "你在Issue【" + issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】下创建的评论中有新的回复";
            }else if(issue.getSpec().getAssignees().contains(needNotifyUser)){
                notifyTitle = "你负责经办的Issue【" + issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】下的评论有新的回复";
            }else if (issue.getSpec().getOwner().equals(needNotifyUser)){
                notifyTitle = "你创建的Issue【" + issue.getSpec().getTitle() + "（" + Issue.parseIssueState(issue.getStatus().getState()) + "）】下的评论有新的回复";
            }else{
                notifyTitle = "你有新的回复";
            }
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(issueReplyComment.getApiVersion())
                .kind(issueReplyComment.getKind())
                .name(issueReplyComment.getMetadata().getName())
                .title(notifyTitle)
                .url(contentUrl)
                .build();
            notificationReasonEmitter.emit(Constant.HAS_NEW_REPLY_ISSUE_COMMENT,
                builder -> {
                    var attributes = IssueReplyCommentCreatedReasonData.builder()
                        .notifyTitle(notifyTitle)
                        .isApproved(issueReplyComment.getSpec().getApproved())
                        .issueReplyCommentCreatedAt(issueReplyComment.getMetadata().getCreationTimestamp()
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                        .issueCommentRawContent(originalIssueComment.getSpec().getContent().getRaw())
                        .issueCommentHTMLContent(originalIssueComment.getSpec().getContent().getHtml())
                        .issueReplyCommentRawContent(issueReplyComment.getSpec().getContent().getRaw())
                        .issueReplyCommentHTMLContent(issueReplyComment.getSpec().getContent().getHtml())
                        .permalink(contentUrl)
                        .receiveOwner(needNotifyUser)
                        .issueReplyCommentOwner(issueReplyComment.getSpec().getOwner())
                        .build();
                    builder.attributes(ReasonDataConverterUtils.toAttributeMap(attributes))
                        .author(UserIdentity.of(issueReplyComment.getSpec().getOwner()))
                        .subject(reasonSubject);
                }).block();

        }

        @Builder
        record IssueReplyCommentCreatedReasonData(String notifyTitle, boolean isApproved, String issueReplyCommentCreatedAt,
                                             String issueCommentRawContent, String issueCommentHTMLContent, String issueReplyCommentRawContent,
                                             String issueReplyCommentHTMLContent, String permalink, String receiveOwner, String issueReplyCommentOwner) {
        }

    }

}
