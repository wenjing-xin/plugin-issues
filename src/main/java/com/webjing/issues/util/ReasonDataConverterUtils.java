package com.webjing.issues.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;
import java.util.Map;

/**
 * @description:
 * @className: NotificationUtils
 * @author: webjing
 * @date: 2025年05月30日 17:56
 */
public class ReasonDataConverterUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> Map<String, Object> toAttributeMap(T data) {
        Assert.notNull(data, "Reason attributes must not be null");
        return OBJECT_MAPPER.convertValue(data, new TypeReference<>() {
        });
    }

}
