package com.webjing.issues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.service.impl.IssueLabelServiceImpl;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

class IssueLabelServiceTest {
    private IssueLabel label(String name, String title) {
        var label = new IssueLabel(); var metadata = new Metadata(); metadata.setName(name); label.setMetadata(metadata);
        var spec = new IssueLabel.IssueLabelSpec(); spec.setLabelName(title); label.setSpec(spec); return label;
    }

    @Test
    void returnsAvailableLabelsWithoutRequiringAnIssueAndPrefersMoreSpecificScope() {
        var client = mock(ReactiveExtensionClient.class);
        var subject = new IssueSubject(); var spec = new IssueSubject.IssueSubjectSpec(); spec.setSubjectType(IssueSubject.SubjectType.PROJECT); subject.setSpec(spec);
        when(client.get(IssueSubject.class, "subject-a")).thenReturn(Mono.just(subject));
        var specific = label("specific", "bug"); var byType = label("type", "bug");
        var global = label("global", "general"); var deleted = label("deleted", "old"); deleted.getMetadata().setDeletionTimestamp(Instant.now());
        when(client.listAll(eq(IssueLabel.class), any(), any())).thenReturn(Flux.just(specific), Flux.just(byType, deleted), Flux.just(global));
        var options = new IssueLabelServiceImpl(client).listSubjectIssueLabels("subject-a", null).block();
        assertThat(options.getIssueLabelOptions()).extracting("value").containsExactly("specific", "global");
        assertThat(global.getSpec().getLabelName()).isEqualTo("general");
    }
}
