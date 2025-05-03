package com.webjing.issues.vo;

import com.webjing.issues.entity.Stats;
import com.webjing.issues.extension.Issue;
import com.webjing.issues.extension.IssueSubject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import run.halo.app.extension.MetadataOperator;

/**
 * issue依托主体数据传输对象
 * @author: webjing
 * @date: 2025年05月03日 13:27
 */
@Data
@SuperBuilder
@ToString
@EqualsAndHashCode
public class IssueSubjectVO {

    private MetadataOperator metadata;

    private IssueSubject.IssueSubjectSpec spec;

    private ContributorVO contributorVo;

    private Stats stats;

    public static IssueSubjectVO from(IssueSubject issueSubject) {
        Assert.notNull(issueSubject, "The issue subject must not be null.");
        return IssueSubjectVO.builder()
            .metadata(issueSubject.getMetadata())
            .spec(issueSubject.getSpec())
            .build();
    }

}
