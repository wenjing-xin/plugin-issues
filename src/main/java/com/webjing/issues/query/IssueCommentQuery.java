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

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.router.QueryParamBuildUtil.sortParameter;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

/**
 * @description: issue 评论查询条件
 * @className: IssueCommentQuery
 * @author: webjing
 * @date: 2025年05月26日 09:57
 */
@Slf4j
public class IssueCommentQuery extends SortableRequest {

    private final MultiValueMap<String, String> queryParams;

    private String username;

    public IssueCommentQuery(ServerWebExchange exchange, String username) {
        this(exchange);
        this.username = username;
    }

    public IssueCommentQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        if (StringUtils.isNotBlank(username)) {
            return username;
        }
        String ownerName = queryParams.getFirst("owner");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "issueMessage approved.")
    public Boolean getApproved() {
        return convertBooleanOrNull(queryParams.getFirst("approved"));
    }
    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
    }


    @Schema(description = "issueName name.")
    public String getIssueName() {
        String issueName = queryParams.getFirst("issueName");
        return StringUtils.isBlank(issueName) ? null : issueName;
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
        if (StringUtils.isNotBlank(getIssueName())) {
            builder.andQuery(equal("spec.issueName", getIssueName()));
        }
        if (getApproved() != null) {
            builder.andQuery(equal("spec.approved", Boolean.toString(getApproved())));
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
                .name("owner")
                .description("IssueComment owner.")
                .implementation(String.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("approved")
                .description("IssueComment approved.")
                .implementation(Boolean.class)
                .required(false))
            .parameter(parameterBuilder()
                .in(ParameterIn.QUERY)
                .name("issueName")
                .description("issue name.")
                .implementation(String.class)
                .required(true));
    }

}
