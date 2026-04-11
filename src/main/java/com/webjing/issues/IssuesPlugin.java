package com.webjing.issues;

import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import java.util.Optional;
import java.util.Set;

/**
* plugin start class
* @author: webjing
* @date: 2025/4/12 22:40
*/
@Component
public class IssuesPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public IssuesPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(IssueSubject.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<IssueSubject, String>single("spec.displayName", String.class)
                .indexFunc(issueSubject -> issueSubject.getSpec().getDisplayName()));
            indexSpecs.add(IndexSpecs.<IssueSubject, String>single("spec.description", String.class)
                .indexFunc(issueSubject -> issueSubject.getSpec().getDescription()));
            indexSpecs.add(IndexSpecs.<IssueSubject, String>single("spec.subjectType", String.class)
                .indexFunc(issueSubject -> issueSubject.getSpec().getSubjectType().name()));
            indexSpecs.add(IndexSpecs.<IssueSubject, String>single("spec.subjectVisible", String.class)
                .indexFunc(issueSubject -> issueSubject.getSpec().getSubjectVisible().name()));
            indexSpecs.add(IndexSpecs.<IssueSubject, String>single("spec.owner", String.class)
                .indexFunc(issueSubject -> issueSubject.getSpec().getOwner()));
            indexSpecs.add(IndexSpecs.<IssueSubject, String>multi("spec.issueTemplates", String.class)
                .indexFunc(issueSubject -> {
                    var templates = issueSubject.getSpec().getIssueTemplates();
                    return templates == null ? Set.of() : templates;
                }));
        });

        schemeManager.register(IssueLabel.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<IssueLabel, String>single("spec.labelName", String.class)
                .indexFunc(issueLabel -> issueLabel.getSpec().getLabelName()));
            indexSpecs.add(IndexSpecs.<IssueLabel, String>single("spec.description", String.class)
                .indexFunc(issueLabel -> issueLabel.getSpec().getDescription()));
            indexSpecs.add(IndexSpecs.<IssueLabel, String>single("spec.scope", String.class)
                .indexFunc(issueLabel -> issueLabel.getSpec().getScope().name()));
            indexSpecs.add(IndexSpecs.<IssueLabel, String>single("spec.subjectType", String.class)
                .indexFunc(issueLabel -> issueLabel.getSpec().getSubjectType().name()));
            indexSpecs.add(IndexSpecs.<IssueLabel, String>single("spec.subjectName", String.class)
                .indexFunc(issueLabel -> issueLabel.getSpec().getSubjectName()));
        });

        schemeManager.register(IssueComment.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<IssueComment, String>single("spec.approved", String.class)
                .indexFunc(issueComment -> {
                    var approved = issueComment.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }));
            indexSpecs.add(IndexSpecs.<IssueComment, String>single("spec.owner", String.class)
                .indexFunc(issueComment -> issueComment.getSpec().getOwner()));
            indexSpecs.add(IndexSpecs.<IssueComment, String>single("spec.issueName", String.class)
                .indexFunc(issueComment -> issueComment.getSpec().getIssueName()));
            indexSpecs.add(IndexSpecs.<IssueComment, String>single("spec.quoteCommentUid", String.class)
                .indexFunc(issueComment -> issueComment.getSpec().getQuoteCommentUid()));
        });

        schemeManager.register(IssueTemplate.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.name", String.class)
                .indexFunc(issueTemplate -> {
                    var name = issueTemplate.getSpec().getName();
                    return name == null ? null : name.toString();
                }));
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.description", String.class)
                .indexFunc(issueTemplate -> issueTemplate.getSpec().getDescription()));
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.owner", String.class)
                .indexFunc(issueTemplate -> issueTemplate.getSpec().getOwner()));
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.scope", String.class)
                .indexFunc(issueTemplate -> issueTemplate.getSpec().getScope().name()));
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.subjectName", String.class)
                .indexFunc(issueTemplate -> issueTemplate.getSpec().getSubjectName()));
            indexSpecs.add(IndexSpecs.<IssueTemplate, String>single("spec.subjectType", String.class)
                .indexFunc(issueTemplate -> issueTemplate.getSpec().getSubjectType().name()));
        });

        schemeManager.register(Issue.class, indexSpecs -> {
            indexSpecs.add(IndexSpecs.<Issue, String>single("status.state", String.class)
                .indexFunc(issue -> {
                    if (issue.getStatus() == null) {
                        return null;
                    }
                    return issue.getStatus().getState() != null
                        ? issue.getStatus().getState().name()
                        : null;
                }));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.title", String.class)
                .indexFunc(issue -> issue.getSpec().getTitle()));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.owner", String.class)
                .indexFunc(issue -> issue.getSpec().getOwner()));
            indexSpecs.add(IndexSpecs.<Issue, String>multi("spec.labels", String.class)
                .indexFunc(issue -> {
                    var labels = issue.getSpec().getLabels();
                    return labels == null ? Set.of() : labels;
                }));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.releaseTime", String.class)
                .indexFunc(issue -> {
                    var releaseTime = issue.getSpec().getReleaseTime();
                    return releaseTime == null ? null : releaseTime.toString();
                }));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.approved", String.class)
                .indexFunc(issue -> {
                    var approved = issue.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }));
            indexSpecs.add(IndexSpecs.<Issue, Boolean>single(Issue.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME, Boolean.class)
                .indexFunc(issue -> {
                    var observedVersion = Optional.ofNullable(issue.getStatus())
                        .map(Issue.IssueStatus::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < issue.getMetadata().getVersion()) {
                        return Boolean.TRUE;
                    }
                    return null;
                }));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.issueTemplate", String.class)
                .indexFunc(issue -> {
                    var issueTemplate = issue.getSpec().getIssueTemplate();
                    return issueTemplate == null ? null : issueTemplate.toString();
                }));
            indexSpecs.add(IndexSpecs.<Issue, String>single("spec.subjectName", String.class)
                .indexFunc(issue -> {
                    var subjectName = issue.getSpec().getSubjectName();
                    return subjectName == null ? null : subjectName.toString();
                }));
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(IssueSubject.class));
        schemeManager.unregister(schemeManager.get(IssueLabel.class));
        schemeManager.unregister(schemeManager.get(IssueComment.class));
        schemeManager.unregister(schemeManager.get(IssueTemplate.class));
        schemeManager.unregister(schemeManager.get(Issue.class));
    }
}
