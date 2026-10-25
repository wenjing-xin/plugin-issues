package com.webjing.issues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

class TemplateSubmissionServiceTest {
    private final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);
    private final TemplateSubmissionService service = new TemplateSubmissionService(client);

    private Issue issue(Map<String, String> values) {
        var issue = new Issue();
        var metadata = new Metadata(); metadata.setName("issue-test"); metadata.setAnnotations(new LinkedHashMap<>(values));
        issue.setMetadata(metadata);
        var spec = new Issue.IssueSpec(); spec.setIssueTemplate("template-test"); issue.setSpec(spec);
        return issue;
    }

    private IssueTemplate.TemplateField field(String key, String parent, String expected, boolean required) {
        var field = new IssueTemplate.TemplateField(); field.setKey(key); field.setTitle(key);
        field.setRequiredMode(required ? "REQUIRED" : "");
        if (parent != null) { var condition = new IssueTemplate.FieldCondition(); condition.setFieldKey(parent); condition.setValue(expected); field.setShowWhen(condition); }
        return field;
    }

    private void template(IssueTemplate.TemplateField... fields) {
        var template = new IssueTemplate(); var spec = new IssueTemplate.IssueTemplateSpec();
        var map = new LinkedHashMap<String, IssueTemplate.TemplateField>();
        for (var field : fields) map.put(field.getKey(), field);
        spec.setFields(map); template.setSpec(spec);
        when(client.get(IssueTemplate.class, "template-test")).thenReturn(Mono.just(template));
    }

    @Test
    void removesHiddenValuesAndDoesNotRequireHiddenFields() {
        template(field("kind", null, null, true), field("steps", "kind", "bug", true), field("details", "steps", "reproduce", true));
        var issue = issue(Map.of("kind", "question", "steps", "reproduce", "details", "stale value", "custom", "keep"));
        service.prepare(issue).block();
        assertThat(issue.getMetadata().getAnnotations()).containsExactlyInAnyOrderEntriesOf(Map.of("kind", "question", "custom", "keep"));
    }

    @Test
    void requiresVisibleFieldsAndAcceptsValidCascades() {
        template(field("kind", null, null, true), field("steps", "kind", "bug", true));
        assertThatThrownBy(() -> service.prepare(issue(Map.of("kind", "bug"))).block()).isInstanceOf(ResponseStatusException.class).hasMessageContaining("steps");
        assertThat(service.prepare(issue(Map.of("kind", "bug", "steps", "reproduce"))).block().getMetadata().getAnnotations()).containsEntry("steps", "reproduce");
    }

    @Test
    void rejectsCyclesAndMissingConditionFields() {
        template(field("a", "b", "x", false), field("b", "a", "x", false));
        assertThatThrownBy(() -> service.prepare(issue(Map.of())).block()).hasMessageContaining("循环引用");
        template(field("a", "missing", "x", false));
        assertThatThrownBy(() -> service.prepare(issue(Map.of())).block()).hasMessageContaining("不存在");
    }
}
