package com.webjing.issues.vo;

import com.webjing.issues.extension.IssueDetail;
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
public class IssueDetailVo {

    private MetadataOperator metadata;

    private IssueDetail.IssueDetailSpec spec;

    private ContributorVo contributorVo;

    private Stats stats;

    public static IssueDetailVo from(IssueDetail issueDetail) {
        Assert.notNull(issueDetail, "The issue detail must not be null.");
        return IssueDetailVo.builder()
            .metadata(issueDetail.getMetadata())
            .spec(issueDetail.getSpec())
            .build();
    }

}
