package com.quickserve.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessException extends BusinessException {

    public TenantAccessException() {
        super("Access denied: resource does not belong to your tenant", HttpStatus.NOT_FOUND);
        // Return 404 not 403 — do not reveal resource existence
    }

    public TenantAccessException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
