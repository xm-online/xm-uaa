package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Security;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Validation of tenant security.defaultClientSecret used by default clients (application.default-client-id).
 */
@Slf4j
public final class DefaultClientSecretPolicy {

    static final String WELL_KNOWN_CLIENT_SECRET = "internal";

    /**
     * Unknown secret used when tenant does not define security.defaultClientSecret,
     * so default clients cannot authenticate with any predictable password.
     */
    private static final String FALLBACK_CLIENT_SECRET = UUID.randomUUID().toString();

    private DefaultClientSecretPolicy() {
    }

    /**
     * Logs misconfiguration and returns secret to be used for default clients.
     */
    public static String resolve(String tenantKey, Security security) {
        String secret = security != null ? security.getDefaultClientSecret() : null;
        validate(tenantKey, secret);
        return StringUtils.isBlank(secret) ? FALLBACK_CLIENT_SECRET : secret;
    }

    public static void validate(String tenantKey, String secret) {
        if (StringUtils.isBlank(secret)) {
            log.error("!!! SECURITY MISCONFIGURATION !!! Tenant '{}': security.defaultClientSecret is not set "
                + "in tenant uaa.yml, random secret is used and default clients can not authenticate. "
                + "Set security.defaultClientSecret in tenant configuration", tenantKey);
        } else if (WELL_KNOWN_CLIENT_SECRET.equals(secret)) {
            log.error("!!! SECURITY MISCONFIGURATION !!! Tenant '{}': security.defaultClientSecret in tenant uaa.yml "
                + "is set to well-known value, default clients can be used by anyone. "
                + "Change security.defaultClientSecret in tenant configuration", tenantKey);
        }
    }
}
