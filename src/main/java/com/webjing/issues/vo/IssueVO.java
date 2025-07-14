package com.webjing.issues.vo;

import com.webjing.issues.entity.IssueStats;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueLabel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import run.halo.app.extension.MetadataOperator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月10日 14:45
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class IssueVO {

    private MetadataOperator metadata;

    private Issue.IssueSpec spec;

    private Issue.IssueStatus status;

    private ContributorVO contributorVo;

    private IssueStats issueStats;

    private List<IssueLabel> issueLabels;

    private List<Map<String, String>> templateData;

    public static IssueVO from(Issue issueMessage) {
        Assert.notNull(issueMessage, "The issue message must not be null.");
        return IssueVO.builder()
            .metadata(issueMessage.getMetadata())
            .spec(issueMessage.getSpec())
            .status(issueMessage.getStatus())
            .build();
    }
}
