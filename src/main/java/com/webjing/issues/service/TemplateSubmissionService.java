package com.webjing.issues.service;

import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.issues.extension.IssueTemplate.TemplateField;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;

/** Applies the same conditional-field contract to Console and frontend submissions. */
@Component
@RequiredArgsConstructor
public class TemplateSubmissionService {
    private final ReactiveExtensionClient client;

    public Mono<Issue> prepare(Issue issue) {
        var templateName = issue.getSpec().getIssueTemplate();
        if (StringUtils.isBlank(templateName)) return Mono.just(issue);
        return client.get(IssueTemplate.class, templateName)
            .map(template -> {
            var fields = template.getSpec().getFields();
            if (fields == null || fields.isEmpty()) return issue;
            var values = new HashMap<String, String>();
            if (issue.getMetadata().getAnnotations() != null) values.putAll(issue.getMetadata().getAnnotations());
            fields.forEach((key, field) -> {
                if (field.getDefaultValue() != null) values.putIfAbsent(key, field.getDefaultValue());
            });
            var visibility = new HashMap<String, Boolean>();
            fields.forEach((key, field) -> visibility.put(key, visible(key, fields, values, new HashSet<>())));
            fields.forEach((key, field) -> {
                if (!visibility.get(key)) {
                    values.remove(key);
                } else if ("required".equalsIgnoreCase(field.getRequiredMode()) && StringUtils.isBlank(values.get(key))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写：" + field.getTitle());
                }
            });
            issue.getMetadata().setAnnotations(values);
            return issue;
        }).switchIfEmpty(Mono.just(issue));
    }

    private boolean visible(String key, Map<String, TemplateField> fields, Map<String, String> values,
                            Set<String> path) {
        if (!path.add(key)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板显示条件存在循环引用");
        var field = fields.get(key);
        if (field == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板显示条件引用了不存在的字段");
        var condition = field.getShowWhen();
        if (condition == null) return true;
        if (StringUtils.isBlank(condition.getFieldKey()) || condition.getValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板显示条件不完整");
        }
        return visible(condition.getFieldKey(), fields, values, path)
            && condition.getValue().equals(values.get(condition.getFieldKey()));
    }
}
