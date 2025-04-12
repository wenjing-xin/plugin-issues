package com.webjing.issues;

import com.webjing.issues.extension.IssueDetail;
import com.webjing.issues.extension.IssueMessage;
import com.webjing.issues.extension.IssueTemplate;
import org.apache.commons.lang3.BooleanUtils;
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

        schemeManager.register(IssueMessage.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.title")
                .setIndexFunc(simpleAttribute(IssueMessage.class,
                    issueMessage -> issueMessage.getSpec().getTitle())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(IssueMessage.class,
                    issueMessage -> issueMessage.getSpec().getOwner())
                )
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.labels")
                .setIndexFunc(multiValueAttribute(IssueMessage.class, issueMessage -> {
                    var labels = issueMessage.getSpec().getLabels();
                    return labels == null ? Set.of() : labels;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.releaseTime")
                .setIndexFunc(simpleAttribute(IssueMessage.class, issueMessage -> {
                    var releaseTime = issueMessage.getSpec().getReleaseTime();
                    return releaseTime == null ? null : releaseTime.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.approved")
                .setIndexFunc(simpleAttribute(IssueMessage.class, issueMessage -> {
                    var approved = issueMessage.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName(IssueMessage.REQUIRE_SYNC_ON_STARTUP_INDEX_NAME)
                .setIndexFunc(simpleAttribute(IssueMessage.class, moment -> {
                    var observedVersion = Optional.ofNullable(moment.getStatus())
                        .map(IssueMessage.IssueMessageStatus::getObservedVersion)
                        .orElse(-1L);
                    if (observedVersion < moment.getMetadata().getVersion()) {
                        return BooleanUtils.TRUE;
                    }
                    // don't care about the false case
                    return null;
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.issueTemplate")
                .setIndexFunc(simpleAttribute(IssueMessage.class, issueMessage -> {
                    var issueTemplate = issueMessage.getSpec().getIssueTemplate();
                    return issueTemplate == null ? null : issueTemplate.toString();
                }))
            );
        });

        schemeManager.register(IssueDetail.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.approved")
                .setIndexFunc(simpleAttribute(IssueDetail.class, issueDetail -> {
                    var approved = issueDetail.getSpec().getApproved();
                    return approved == null ? null : approved.toString();
                }))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.owner")
                .setIndexFunc(simpleAttribute(IssueDetail.class, issueDetail ->
                    issueDetail.getSpec().getOwner())
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
        });
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(IssueMessage.class));
        schemeManager.unregister(schemeManager.get(IssueDetail.class));
        schemeManager.unregister(schemeManager.get(IssueTemplate.class));
    }
}
