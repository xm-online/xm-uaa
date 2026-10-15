package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Security;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validation of tenant security.defaultClientSecret used by default clients (application.default-client-id).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultClientSecretSupplier {

    static final String WELL_KNOWN_CLIENT_SECRET = "internal";
    private static final String FALLBACK_CLIENT_SECRET = UUID.randomUUID().toString();

    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;

    public String resolve() {
        return resolve(tenantContextHolder.getTenantKey(), tenantPropertiesService.getTenantProps());
    }

    public String resolve(String tenantKey) {
        return resolve(tenantKey, tenantPropertiesService.getTenantProps(TenantKey.valueOf(tenantKey)));
    }

    private static String resolve(String tenantKey, TenantProperties tenantProperties) {
        Security security = tenantProperties.getSecurity();
        String secret = security != null ? security.getDefaultClientSecret() : null;
        validate(tenantKey, secret);
        return secretInUse(security);
    }

    private static String secretInUse(Security security) {
        String secret = security != null ? security.getDefaultClientSecret() : null;
        return StringUtils.isBlank(secret) ? FALLBACK_CLIENT_SECRET : secret;
    }

    public boolean isWellKnownSecretInUse(String tenant) {
        return WELL_KNOWN_CLIENT_SECRET.equals(resolve(tenant));
    }

    private static void validate(String tenantKey, String secret) {
        if (StringUtils.isBlank(secret)) {
            log.error("!!! MISCONFIGURATION !!! Tenant '{}': security.defaultClientSecret is not set "
                + "in tenant uaa.yml, random secret is used and default clients can not authenticate. "
                + "Set security.defaultClientSecret in tenant configuration", tenantKey);
        } else if (WELL_KNOWN_CLIENT_SECRET.equals(secret)) {
            log.error("!!! MISCONFIGURATION !!! Tenant '{}': security.defaultClientSecret in tenant uaa.yml "
                + "is set to well-known value, default clients can be used by anyone. "
                + "Change security.defaultClientSecret in tenant configuration", tenantKey);
        }
    }
}
