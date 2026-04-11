package com.webjing.issues.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 404 异常处理
 * @author: webjing
 * @date: 2025年03月10日 11:59
 */
public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }

}
