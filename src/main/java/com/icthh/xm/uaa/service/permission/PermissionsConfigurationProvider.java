package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A service that provides {@link ConfigurationSource} based on the configuration.
 * Also handles updates for Roles and Permissions.
 *
 * To retrieve data uses the first relevant source, for update applies to all the source
 * in appropriate order.
 */
@Component
@Slf4j
public class PermissionsConfigurationProvider {

    /**
     * Default configuration source
     */
    public static final PermissionsConfigMode DEFAULT_SOURCE = PermissionsConfigMode.CONFIGURATION_SERVICE;

    private final Map<PermissionsConfigMode, ConfigurationSource> sourceMap;
    private final PermissionsConfigModeProvider permissionsConfigModeProvider;

    public PermissionsConfigurationProvider(@Autowired List<ConfigurationSource> sources,
                                            PermissionsConfigModeProvider permissionsConfigModeProvider) {
        sourceMap = sources.stream()
            .collect(Collectors.toMap(ConfigurationSource::getMode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        this.permissionsConfigModeProvider = permissionsConfigModeProvider;
        Assert.notNull(getDefaultSource(), "Default data source is not specified");
    }

    /**
     * @return configuration source to be used.
     */
    public ConfigurationSource getSource() {
        PermissionsConfigMode mode = getModeForTenantOrDefault();
        log.debug("Using mode {}", mode);
        return sourceMap.get(mode);
    }

    /**
     * @return default configuration source
     */
    public ConfigurationSource getDefaultSource() {
        return sourceMap.get(DEFAULT_SOURCE);
    }

    /**
     * Updates roles according to {@code roles}. This is a full state update.
     * @param roles role configuration, key - role key, value - role itself.
     */
    public void updateRoles(Map<String, Role> roles) {
        sourceMap.values().forEach(s -> s.updateRoles(roles));
    }

    /**
     * Updates permissions. This is a full state update.
     * @param permissions permission configuration, where key - application name,
     *                    inner map key - role key, value - permission.
     */
    public void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions) {
        sourceMap.values().forEach(s -> s.updatePermissions(permissions));
    }

    private PermissionsConfigMode getModeForTenantOrDefault() {
        PermissionsConfigMode mode = permissionsConfigModeProvider.getMode();
        log.debug("Mode from a tenant configuration {}", mode);

        return Objects.requireNonNullElse(mode, PermissionsConfigMode.CONFIGURATION_SERVICE);
    }
}

