package com.webjing.issues.query;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.IListRequest;
import run.halo.app.extension.router.SortableRequest;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.greaterThan;
import static run.halo.app.extension.index.query.Queries.lessThan;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

/**
* issueMessage 的查询构建器
* @author: webjing
* @date: 2025/3/6 15:02
*/
@Slf4j
public class IssueQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;

    private String username;

    public IssueQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    public IssueQuery(ServerWebExchange exchange, String username) {
        this(exchange);
        this.username = username;
    }

    @Nullable
    @Schema(description = "IssueMessage filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        if (StringUtils.isNotBlank(username)) {
            return username;
        }
        String ownerName = queryParams.getFirst("ownerName");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "subject name.")
    public String getSubjectName() {
        String subjectName = queryParams.getFirst("subjectName");
        return StringUtils.isBlank(subjectName) ? null : subjectName;
    }

    @Schema(description = "IssueTemplate name.")
    public String getIssueTemplate() {
        String issueTemplate = queryParams.getFirst("issueTemplate");
        return StringUtils.isBlank(issueTemplate) ? null : issueTemplate;
    }


    @Schema(description = "Issue message label.")
    public String getLabel() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("label"), null);
    }

    @Schema
    public Instant getStartDate() {
        String startDate = queryParams.getFirst("startDate");
        return convertInstantOrNull(startDate);
    }

    @Schema
    public Instant getEndDate() {
        String endDate = queryParams.getFirst("endDate");
        return convertInstantOrNull(endDate);
    }

    /**
     * Build {@link ListOptions} from query params.
     *
     * @return a list options.
     */
    public ListOptions toListOptions() {
        var builder = ListOptions.builder(
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector())
        );
        if (StringUtils.isNotBlank(getOwnerName())) {
            builder.andQuery(equal("spec.owner", getOwnerName()));
        }
        if (StringUtils.isNotBlank(getLabel())) {
            builder.andQuery(equal("spec.labels", getLabel()));
        }
        if (getApproved() != null) {
            builder.andQuery(equal("spec.approved", Boolean.toString(getApproved())));
        }

        if (getStartDate() != null) {
            builder.andQuery(greaterThan("spec.releaseTime", getStartDate().toString(), true));
        }
        if (getEndDate() != null) {
            builder.andQuery(lessThan("spec.releaseTime", getEndDate().toString(), true));
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            builder.andQuery(contains("spec.title", getKeyword()));
        }
        if (StringUtils.isNotBlank(getIssueTemplate())) {
            builder.andQuery(contains("spec.issueTemplate", getIssueTemplate()));
        }
        if (StringUtils.isNotBlank(getSubjectName())) {
            builder.andQuery(contains("spec.subjectName", getSubjectName()));
        }
        return builder.build();
    }

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("spec.releaseTime").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }

    @Schema(description = "issueMessage approved.")
    public Boolean getApproved() {
        return convertBooleanOrNull(queryParams.getFirst("approved"));
    }

    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
    }

    private Instant convertInstantOrNull(String timeStr) {
        return StringUtils.isBlank(timeStr) ? null : Instant.parse(timeStr);
    }

    public static void buildParameters(Builder builder) {
        IListRequest.buildParameters(builder);
        builder.parameter(sortParameter())
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("keyword")
                .description("IssueMessages filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectName")
                .description("the subject of issue")
                .implementation(String.class)
                .required(true))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("ownerName")
                .description("Owner name.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("label")
                .description("IssueMessages label.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("startDate")
                .implementation(Instant.class)
                .description("IssueMessages start date.")
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("endDate")
                .implementation(Instant.class)
                .description("IssueMessages end date.")
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("approved")
                .description("IssueMessages approved.")
                .implementation(Boolean.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("issueTemplate")
                .description("IssueMessages’s issueTemplate.")
                .implementation(String.class)
                .required(false))
        ;
    }

}
