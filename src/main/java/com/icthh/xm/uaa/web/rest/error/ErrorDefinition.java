package com.icthh.xm.uaa.web.rest.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A translatable error: the stable code a client branches on, plus the text to fall back to when the tenant
 * has no {@code i18n-message.yml} entry for that code.
 */
@Getter
@RequiredArgsConstructor
public class ErrorDefinition {

    private final String code;
    private final String defaultMessage;
}
