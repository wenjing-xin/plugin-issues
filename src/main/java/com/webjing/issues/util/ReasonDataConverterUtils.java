package com.webjing.issues.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.Assert;
import run.halo.app.infra.utils.JsonUtils;
import java.util.Map;

/**
 * @description:
 * @className: NotificationUtils
 * @author: webjing
 * @date: 2025年05月30日 17:56
 */
public class ReasonDataConverterUtils {

    public static <T> Map<String, Object> toAttributeMap(T data) {
        Assert.notNull(data, "Reason attributes must not be null");
        return JsonUtils.mapper().convertValue(data, new TypeReference<>() {
        });
    }

}
