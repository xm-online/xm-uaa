package com.icthh.xm.uaa.security;

import org.springframework.security.core.AuthenticationException;

/**
 * This exception is thrown in case of a user trying to authenticate without providing domain.
 */
public class TenantNotProvidedException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public TenantNotProvidedException(String message) {
        super(message);
    }

    public TenantNotProvidedException(String message, Throwable t) {
        super(message, t);
    }
}
