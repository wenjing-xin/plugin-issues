package com.webjing.issues.query;

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
import run.halo.app.extension.router.selector.FieldSelector;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.contains;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

/**
 * @description: issue标签查询
 * @className: IssueLabelQuery
 * @author: webjing
 * @date: 2025年06月26日 09:28
 */
@Slf4j
public class IssueLabelQuery extends SortableRequest {


    private final MultiValueMap<String, String> queryParams;

    public IssueLabelQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    @Nullable
    @Schema(description = "IssueLabel filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }


    @Schema(description = "issuelable isGlobal")
    public Boolean getIsGlobal() {
        return convertBooleanOrNull(queryParams.getFirst("isGlobal"));
    }

    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
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
        var listOptions = labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();

        if (listOptions.getFieldSelector() != null) {
            query = and(query, listOptions.getFieldSelector().query());
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            query = and(query, contains("spec.labelName", getKeyword()));
            query = and(query, contains("spec.description", getKeyword()));
        }
        if (getIsGlobal() != null) {
            query = and(query, equal("spec.isGlobal", Boolean.toString(getIsGlobal())));
        }
        if (StringUtils.isNotBlank(getSubjectName())) {
            query = and(query, equal("spec.subjectName", getSubjectName()));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
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
                .description("IssueLabels filtered by keyword.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("subjectName")
                .description("subject name.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("isGlobal")
                .description("label is global")
                .implementation(Boolean.class)
                .required(false))
        ;
    }

}
