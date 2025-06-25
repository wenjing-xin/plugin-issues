package com.webjing.issues.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.ServerRequest;
import java.time.Instant;
import java.time.ZoneId;

/**
 * @description:
 * @className: HaloUtils
 * @author: webjing
 * @date: 2025年06月24日 10:42
 */
@Slf4j
@UtilityClass
public class HaloUtils {


    /**
     * Gets user-agent from server request.
     *
     * @param request server request
     * @return user-agent string if found, otherwise "unknown"
     */
    public static String userAgentFrom(ServerRequest request) {
        HttpHeaders httpHeaders = request.headers().asHttpHeaders();
        // https://en.wikipedia.org/wiki/User_agent
        String userAgent = httpHeaders.getFirst(HttpHeaders.USER_AGENT);
        if (StringUtils.isBlank(userAgent)) {
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-CH-UA
            userAgent = httpHeaders.getFirst("Sec-CH-UA");
        }
        return StringUtils.defaultIfBlank(userAgent, "unknown");
    }

    public static String getDayText(Instant instant) {
        Assert.notNull(instant, "Instant must not be null");
        int dayValue = instant.atZone(ZoneId.systemDefault()).getDayOfMonth();
        return StringUtils.leftPad(String.valueOf(dayValue), 2, '0');
    }

    public static String getMonthText(Instant instant) {
        Assert.notNull(instant, "Instant must not be null");
        int monthValue = instant.atZone(ZoneId.systemDefault()).getMonthValue();
        return StringUtils.leftPad(String.valueOf(monthValue), 2, '0');
    }

    public static String getYearText(Instant instant) {
        Assert.notNull(instant, "Instant must not be null");
        return String.valueOf(instant.atZone(ZoneId.systemDefault()).getYear());
    }


}
