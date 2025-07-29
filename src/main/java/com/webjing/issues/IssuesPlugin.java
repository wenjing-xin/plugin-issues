package com.webjing.issues;

import com.webjing.issues.extension.IssueComment;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
import com.webjing.mandate.auth.WebjingPluginAuthManager;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import java.util.Optional;
import java.util.Set;

import static run.halo.app.extension.index.IndexAttributeFactory.multiValueAttribute;
import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

/**
* plugin start class
* @author: webjing
* @date: 2025/4/12 22:40
*/
@Component
public class IssuesPlugin extends BasePlugin {

    // @Autowired
    // private WebjingPluginAuthManager webjingPluginAuthManager;

    private final SchemeManager schemeManager;

    public IssuesPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {

        // webjingPluginAuthManager.pluginStartCheck();

        schemeManager.register(IssueSubject.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.displayName")
                .setIndexFunc(simpleAttribute(IssueSubject.class,
                    issueSubject -> issueSubject.getSpec().getDisplayName())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.description")
                .setIndexFunc(simpleAttribute(IssueSubject.class,
                    issueSubject -> issueSubject.getSpec().getDescription())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectType")
                .setIndexFunc(simpleAttribute(IssueSubject.class,
                    issueSubject -> issueSubject.getSpec().getSubjectType().name())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectVisible")
                .setIndexFunc(simpleAttribute(IssueSubject.class,
                    issueSubject -> issueSubject.getSpec().getSubjectVisible().name())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(IssueSubject.class,
                    issueSubject -> issueSubject.getSpec().getOwner())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.issueTemplates")
                .setIndexFunc(multiValueAttribute(IssueSubject.class, issueSubject -> {
                    var templates = issueSubject.getSpec().getIssueTemplates();
                    return templates == null ? Set.of() : templates;
                }))
            );
        });

        schemeManager.register(IssueLabel.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.labelName")
                .setIndexFunc(simpleAttribute(IssueLabel.class,
                    issueLabel -> issueLabel.getSpec().getLabelName())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.description")
                .setIndexFunc(simpleAttribute(IssueLabel.class,
                    issueLabel -> issueLabel.getSpec().getDescription())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.scope")
                .setIndexFunc(simpleAttribute(IssueLabel.class,
                    issueLabel ->  issueLabel.getSpec().getScope().name())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectType")
                .setIndexFunc(simpleAttribute(IssueLabel.class,
                    issueLabel -> issueLabel.getSpec().getSubjectType().name())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectName")
                .setIndexFunc(simpleAttribute(IssueLabel.class,
                    issueLabel -> issueLabel.getSpec().getSubjectName())
                )
            );
        });
        schemeManager.register(IssueComment.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.approved")
                .setIndexFunc(simpleAttribute(IssueComment.class, issueComment -> {
                    var approved = issueComment.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(IssueComment.class, issueComment ->
                    issueComment.getSpec().getOwner())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.issueName")
                .setIndexFunc(simpleAttribute(IssueComment.class, issueComment ->
                    issueComment.getSpec().getIssueName())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.quoteCommentUid")
                .setIndexFunc(simpleAttribute(IssueComment.class, issueComment ->
                    issueComment.getSpec().getQuoteCommentUid())
                )
            );
        });

        schemeManager.register(IssueTemplate.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.name")
                .setIndexFunc(simpleAttribute(IssueTemplate.class, issueTemplate -> {
                    var name = issueTemplate.getSpec().getName();
                    return name == null ? null : name.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.description")
                .setIndexFunc(simpleAttribute(IssueTemplate.class, issueTemplate ->
                    issueTemplate.getSpec().getDescription())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(IssueTemplate.class, issueTemplate ->
                    issueTemplate.getSpec().getOwner())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.scope")
                .setIndexFunc(simpleAttribute(IssueTemplate.class,
                    issueTemplate ->  issueTemplate.getSpec().getScope().name())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectName")
                .setIndexFunc(simpleAttribute(IssueTemplate.class,
                    issueTemplate ->  issueTemplate.getSpec().getSubjectName())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectType")
                .setIndexFunc(simpleAttribute(IssueTemplate.class,
                    issueTemplate ->  issueTemplate.getSpec().getSubjectType().name())
                )
            );
        });
        schemeManager.register(Issue.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("status.state")
                .setIndexFunc(simpleAttribute(Issue.class,
                    issue ->  {
                        if(issue.getStatus() == null){
                            return null;
                        }else{
                            return  issue.getStatus().getState() != null ? issue.getStatus().getState().name() : null;
                        }
                    })
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.title")
                .setIndexFunc(simpleAttribute(Issue.class,
                    issue -> issue.getSpec().getTitle())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(Issue.class,
                    issue -> issue.getSpec().getOwner())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.labels")
                .setIndexFunc(multiValueAttribute(Issue.class, issue -> {
                    var labels = issue.getSpec().getLabels();
                    return labels == null ? Set.of() : labels;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.releaseTime")
                .setIndexFunc(simpleAttribute(Issue.class, issue -> {
                    var releaseTime = issue.getSpec().getReleaseTime();
                    return releaseTime == null ? null : releaseTime.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.approved")
                .setIndexFunc(simpleAttribute(Issue.class, issue -> {
                    var approved = issue.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName(Issue.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(Issue.class, issue -> {
                    var observedVersion = Optional.ofNullable(issue.getStatus())
                        .map(Issue.IssueStatus::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < issue.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.issueTemplate")
                .setIndexFunc(simpleAttribute(Issue.class, issue -> {
                    var issueTemplate = issue.getSpec().getIssueTemplate();
                    return issueTemplate == null ? null : issueTemplate.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.subjectName")
                .setIndexFunc(simpleAttribute(Issue.class, issue -> {
                    var subjectName = issue.getSpec().getSubjectName();
                    return subjectName == null ? null : subjectName.toString();
                }))
            );
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
