package com.icthh.xm.uaa.config.tenant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Holds information about incoming user.
 */
@RequiredArgsConstructor
@Getter
public class TenantInfo {

    private final String tenant;
    private final String userLogin;
    private final String userKey;
    private final String protocol;
    private final String domain;
    private final String port;
    private final String webapp;
}
