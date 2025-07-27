package com.webjing.issues.search;

import com.webjing.issues.Constant;
import com.webjing.issues.extension.Issue;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.search.HaloDocument;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @description:
 * @className: DocumentConverter
 * @author: webjing
 * @date: 2025年07月27日 23:16
 */
@Component
@RequiredArgsConstructor
public class IssueDocumentConverter implements Converter<Issue, Mono<HaloDocument>> {

    private final ReactiveExtensionClient client;

    private final ExternalUrlSupplier externalUrlSupplier;

    private final DateTimeFormatter dateFormat =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    @NonNull
    public Mono<HaloDocument> convert(Issue issue) {
        var haloDoc = new HaloDocument();
        var momentContent = issue.getSpec().getContent();
        haloDoc.setMetadataName(issue.getMetadata().getName());
        haloDoc.setType(Constant.ISSUE_DOCUMENT_TYPE);
        haloDoc.setId(haloDocId(issue));
        haloDoc.setDescription(momentContent.getHtml());
        haloDoc.setExposed(issue.getSpec().getApproved());
        haloDoc.setContent(momentContent.getHtml());
        var labels = issue.getSpec().getLabels();
        Optional.ofNullable(labels).ifPresent((label) -> haloDoc.setTags(label.stream().toList()));
        haloDoc.setOwnerName(issue.getSpec().getOwner());
        haloDoc.setUpdateTimestamp(issue.getSpec().getReleaseTime());
        haloDoc.setCreationTimestamp(issue.getMetadata().getCreationTimestamp());
        haloDoc.setPermalink(String.valueOf(externalUrlSupplier.get().resolve(issue.getStatus().getPermalink())));
        haloDoc.setPublished(true);

        return Mono.when(getTitle(issue).doOnNext(haloDoc::setTitle))
            .then(Mono.fromSupplier(() -> haloDoc));
    }

    public String haloDocId(Issue issue) {
        return Constant.ISSUE_DOCUMENT_TYPE + '-' + issue.getMetadata().getName();
    }

    private Mono<String> getTitle(Issue issue) {
        return client.fetch(User.class, issue.getSpec().getOwner())
            .map(user -> user.getSpec().getDisplayName())
            .map(displayName -> {
                ZonedDateTime zonedDateTime =
                    issue.getSpec().getReleaseTime().atZone(ZoneId.systemDefault());
                return "【Issue：" + issue.getSpec().getTitle() + "】" + dateFormat.format(zonedDateTime);
            });
    }

}
