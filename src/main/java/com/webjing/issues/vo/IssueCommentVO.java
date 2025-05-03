package com.webjing.issues.vo;

import com.webjing.issues.entity.Stats;
import com.webjing.issues.extension.IssueComment;
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
 * @date: 2025年04月02日 14:47
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class IssueCommentVO {

    private MetadataOperator metadata;

    private IssueComment.IssueCommentSpec spec;

    private ContributorVO contributorVo;

    private Stats stats;

    public static IssueCommentVO from(IssueComment issueDetail) {
        Assert.notNull(issueDetail, "The issue detail must not be null.");
        return IssueCommentVO.builder()
            .metadata(issueDetail.getMetadata())
            .spec(issueDetail.getSpec())
            .build();
    }

}
