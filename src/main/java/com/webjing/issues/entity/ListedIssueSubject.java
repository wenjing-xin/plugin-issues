package com.webjing.issues.entity;

import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.vo.ContributorVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * issue 主体列表对象
 * @author: webjing
 * @date: 2025年05月03日 15:45
 */
@Data
@Builder
public class ListedIssueSubject {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueSubject issueSubject;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ContributorVO createOwner;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ContributorVO> participateUsers;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private IssueSubjectStats issueSubjectStats;

}
