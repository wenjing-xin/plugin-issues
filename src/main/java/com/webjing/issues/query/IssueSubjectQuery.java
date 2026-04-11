package com.webjing.issues.query;

import com.webjing.issues.extension.IssueSubject;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
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
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

/**
 * issue 依托主体查询参数构建器
 * @author: webjing
 * @date: 2025年05月03日 13:08
 */
@Slf4j
public class IssueSubjectQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;

    public IssueSubjectQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    @Nullable
    @Schema(description = "IssueSubject filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        String ownerName = queryParams.getFirst("owner");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "subject type.")
    public String getSubjectType() {
        String subjectType = queryParams.getFirst("subjectType");
        return StringUtils.isBlank(subjectType) ? null : subjectType;
    }

    @Schema(description = "subject visible.")
    public String getSubjectVisible() {
        String subjectVisible = queryParams.getFirst("subjectVisible");
        return StringUtils.isBlank(subjectVisible) ? null : subjectVisible;
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
        if (StringUtils.isNotBlank(getKeyword())) {
            builder.andQuery(contains("spec.displayName", getKeyword()));
            builder.andQuery(contains("spec.description", getKeyword()));
        }
        if (StringUtils.isNotBlank(getSubjectType())) {
            builder.andQuery(equal("spec.subjectType", getSubjectType()));
        }
        if (StringUtils.isNotBlank(getSubjectVisible())) {
            builder.andQuery(equal("spec.subjectVisible", getSubjectVisible()));
        }
        return builder.build();
    }

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("metadata.creationTimestamp").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
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
                .name("owner")
                .description("IssueTemplate owner.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectType")
                .description("subject type.")
                .implementation(IssueSubject.SubjectType.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectVisible")
                .description("subject visible.")
                .implementation(IssueSubject.SubjectVisible.class)
                .required(false))
        ;
    }

}
