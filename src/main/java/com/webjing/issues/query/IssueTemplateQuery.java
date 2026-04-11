package com.webjing.issues.query;

import com.webjing.issues.extension.IssueLabel;
import com.webjing.issues.extension.IssueSubject;
import com.webjing.issues.extension.IssueTemplate;
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
 * 功能描述
 *
 * @author: webjing
 * @date: 2025年03月17日 11:53
 */
@Slf4j
public class IssueTemplateQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;

    public IssueTemplateQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    @Nullable
    @Schema(description = "IssueTemplate filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        String ownerName = queryParams.getFirst("ownerName");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "issueTemplate scope")
    public String getTemplateScope() {
        return queryParams.getFirst("scope");
    }

    @Schema(description = "subject type")
    public String getSubjectType() {
        String subjectType = queryParams.getFirst("subjectType");
        return StringUtils.isBlank(subjectType) ? null : subjectType;
    }

    @Schema(description = "subject name.")
    public String getSubjectName() {
        String subjectName = queryParams.getFirst("subjectName");
        return StringUtils.isBlank(subjectName) ? null : subjectName;
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

        if (StringUtils.isNotBlank(getTemplateScope())) {
            builder.andQuery(equal("spec.scope", getTemplateScope()));
        }

        if (StringUtils.isNotBlank(getSubjectName())) {
            builder.andQuery(equal("spec.subjectName", getSubjectName()));
        }

        if (StringUtils.isNotBlank(getSubjectType())) {
            builder.andQuery(equal("spec.subjectType", getSubjectType()));
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            builder.andQuery(contains("spec.name", getKeyword()));
            builder.andQuery(contains("spec.description", getKeyword()));
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
                .name("scope")
                .description("template scope")
                .implementation(IssueTemplate.IssueTemplateScope.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectName")
                .description("subject name.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectType")
                .description("subject type.")
                .implementation(IssueSubject.SubjectType.class)
                .required(false))
        ;
    }

}
