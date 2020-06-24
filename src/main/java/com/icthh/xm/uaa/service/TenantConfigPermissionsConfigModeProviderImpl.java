package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link PermissionsConfigModeProvider} based on tenant configuration.
 */
@Component
@RequiredArgsConstructor
public class TenantConfigPermissionsConfigModeProviderImpl implements PermissionsConfigModeProvider {

    public static final String UAA_PERMISSIONS_PROPERTY = "uaa-permissions";
    private final TenantConfigService tenantConfigService;

    @Override
    public PermissionsConfigMode getMode() {
        return Optional.ofNullable(tenantConfigService.getConfig().get(UAA_PERMISSIONS_PROPERTY))
            .map(e -> Boolean.parseBoolean((String) e))
            .orElse(Boolean.FALSE)
            ? PermissionsConfigMode.DATABASE
            : null;
    }
}
