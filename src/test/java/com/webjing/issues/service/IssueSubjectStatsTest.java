package com.webjing.issues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.webjing.issues.entity.IssueLabelOptions;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.notify.NotificationSubscriptionHelper;
import com.webjing.issues.service.impl.IssueSubjectServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

class IssueSubjectStatsTest {
    @Test
    void countsUnusedAvailableLabelsAndKeepsModerationSeparateFromPublishedCounts() {
        var client = mock(ReactiveExtensionClient.class);
        var labels = mock(IssueLabelService.class);
        var options = new IssueLabelOptions();
        options.setIssueLabelOptions(List.of(IssueLabelOptions.IssueLabelItem.builder().value("unused-label").label("bug").build()));
        when(labels.listSubjectIssueLabels("subject-a", null)).thenReturn(Mono.just(options));
        var service = new IssueSubjectServiceImpl(client, mock(RoleService.class), labels, mock(NotificationSubscriptionHelper.class));
        when(client.listAll(eq(Issue.class), any(), any())).thenReturn(Flux.empty());
        assertThat(service.fetchIssueSubjectStats("subject-a").block().getLabels()).isEqualTo(1);
        when(client.listAll(eq(Issue.class), any(), any())).thenReturn(Flux.just(issue(true), issue(false)));
        var stats = service.fetchIssueSubjectStats("subject-a").block();
        assertThat(stats.getTotalIssue()).isEqualTo(1);
        assertThat(stats.getAwaitApproved()).isEqualTo(1);
        assertThat(stats.getLabels()).isEqualTo(1);
    }

    private Issue issue(boolean approved) {
        var issue = new Issue(); var metadata = new Metadata(); metadata.setName("issue-" + approved); issue.setMetadata(metadata);
        var spec = new Issue.IssueSpec(); spec.setApproved(approved); issue.setSpec(spec);
        var status = new Issue.IssueStatus(); status.setState(Issue.IssueState.AWAIT); issue.setStatus(status);
        return issue;
    }
}
