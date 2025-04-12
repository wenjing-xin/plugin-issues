package com.webjing.issues.notify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.webjing.issues.Constant;
import com.webjing.issues.event.IssueMessageHasNewCommentEvent;
import com.webjing.issues.extension.IssueMessage;
import io.micrometer.common.util.StringUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.Ref;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.utils.JsonUtils;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月07日 20:58
 */
@Component
@RequiredArgsConstructor
public class IssueMessageCommentNotificationReasonPublisher {

    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public static final String NEW_COMMENT_ON_ISSUE_MESSAGE = "new-comment-on-issue-message";
    public static final String NOTIFIED_ANNO = Constant.GROUP + "/notified";

    private final ExtensionClient client;
    private final NotificationReasonEmitter notificationReasonEmitter;
    private final ExternalLinkProcessor externalLinkProcessor;

    /**
     * On new comment.
     */
    @Async
    @EventListener(IssueMessageHasNewCommentEvent.class)
    public void onNewComment(IssueMessageHasNewCommentEvent event) {
        Comment comment = event.getComment();
        var annotations = MetadataUtil.nullSafeAnnotations(comment);
        if (annotations.containsKey(NOTIFIED_ANNO)) {
            return;
        }
        publishReasonBy(comment);
        markAsNotified(comment.getMetadata().getName());
    }

    private void markAsNotified(String commentName) {
        client.fetch(Comment.class, commentName).ifPresent(latestComment -> {
            MetadataUtil.nullSafeAnnotations(latestComment).put(NOTIFIED_ANNO, "true");
            client.update(latestComment);
        });
    }

    public void publishReasonBy(Comment comment) {
        Ref subjectRef = comment.getSpec().getSubjectRef();
        var issueMessage = client.fetch(IssueMessage.class, subjectRef.getName()).orElseThrow();
        if (doNotEmitReason(comment, issueMessage)) {
            return;
        }

        String issueUrl =
            externalLinkProcessor.processLink("/issues/" + issueMessage.getMetadata().getName());
        var reasonSubject = Reason.Subject.builder()
            .apiVersion(issueMessage.getApiVersion())
            .kind(issueMessage.getKind())
            .title("issue：" + issueMessage.getMetadata().getName())
            .name(subjectRef.getName())
            .url(issueUrl)
            .build();

        var issueContent =
            defaultIfNull(issueMessage.getSpec().getContent(), new IssueMessage.IssueContent());
        var owner = comment.getSpec().getOwner();
        notificationReasonEmitter.emit(NEW_COMMENT_ON_ISSUE_MESSAGE,
            builder -> {
                var attributes = CommentOnIssueMessageReasonData.builder()
                    .issueMessageName(issueMessage.getMetadata().getName())
                    .issueMessageOwner(issueMessage.getSpec().getOwner())
                    .issueMessageCreatedAt(
                        DEFAULT_DATE_FORMATTER.format(issueMessage.getMetadata().getCreationTimestamp()))
                    .issueMessageHtmlContent(cleanHtmlTag(issueContent.getHtml(), Safelist.basic()))
                    .issueMessageRawContent(cleanHtmlTag(issueContent.getRaw(), Safelist.simpleText()))
                    .issueMessageUrl(issueUrl)
                    .commenter(owner.getDisplayName())
                    .content(comment.getSpec().getContent())
                    .commentName(comment.getMetadata().getName())
                    .build();
                builder.attributes(toAttributeMap(attributes))
                    .author(identityFrom(owner))
                    .subject(reasonSubject);
            }).block();
    }

    static String cleanHtmlTag(String html, Safelist safelist) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return Jsoup.clean(html, safelist);
    }

    static <T> Map<String, Object> toAttributeMap(T data) {
        Assert.notNull(data, "Reason attributes must not be null");
        return JsonUtils.mapper().convertValue(data, new TypeReference<>() {
        });
    }

    static UserIdentity identityFrom(Comment.CommentOwner owner) {
        if (Comment.CommentOwner.KIND_EMAIL.equals(owner.getKind())) {
            return UserIdentity.anonymousWithEmail(owner.getName());
        }
        return UserIdentity.of(owner.getName());
    }

    boolean doNotEmitReason(Comment comment, IssueMessage issueMessage) {
        Comment.CommentOwner commentOwner = comment.getSpec().getOwner();
        String kind = commentOwner.getKind();
        String name = commentOwner.getName();
        var issueMessageOwner = issueMessage.getSpec().getOwner();
        if (Comment.CommentOwner.KIND_EMAIL.equals(kind)) {
            return false;
        }
        return name.equals(issueMessageOwner);
    }

    @Builder
    record CommentOnIssueMessageReasonData(String issueMessageName, String issueMessageOwner, String issueMessageCreatedAt,
                                     String issueMessageHtmlContent, String issueMessageRawContent,
                                     String issueMessageUrl, String commenter, String content,
                                     String commentName) {
    }

}
