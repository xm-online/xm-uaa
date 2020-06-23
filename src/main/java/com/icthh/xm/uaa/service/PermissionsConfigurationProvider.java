package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * //todo V!: add javadocs
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
        Assert.assertNotNull("Default data source is not specified", getDefaultSource());
    }

    public ConfigurationSource getSource() {
        PermissionsConfigMode mode = getModeForTenantOrDefault();
        log.debug("Using mode {}", mode);
        return sourceMap.get(mode);
    }

    public ConfigurationSource getDefaultSource() {
        return sourceMap.get(DEFAULT_SOURCE);
    }


    private PermissionsConfigMode getModeForTenantOrDefault() {
        PermissionsConfigMode mode = permissionsConfigModeProvider.getMode();
        log.debug("Mode from a tenant configuration {}", mode);

        return Objects.requireNonNullElse(mode, PermissionsConfigMode.CONFIGURATION_SERVICE);
    }

    public void updateRoles(Map<String, Role> roles) {
        sourceMap.values().forEach(s -> s.updateRoles(roles));
    }

    public void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions) {
        sourceMap.values().forEach(s -> s.updatePermissions(permissions));
    }
}

