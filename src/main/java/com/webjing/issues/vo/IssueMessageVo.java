package com.webjing.issues.vo;

import com.webjing.issues.extension.IssueMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import run.halo.app.extension.MetadataOperator;

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
public class IssueMessageVo {

    private MetadataOperator metadata;

    private IssueMessage.IssueMessageSpec spec;

    private IssueMessage.IssueMessageStatus status;

    private ContributorVo contributorVo;

    private Stats stats;

    public static IssueMessageVo from(IssueMessage issueMessage) {
        Assert.notNull(issueMessage, "The issue message must not be null.");
        return IssueMessageVo.builder()
            .metadata(issueMessage.getMetadata())
            .spec(issueMessage.getSpec())
            .status(issueMessage.getStatus())
            .build();
    }
}
