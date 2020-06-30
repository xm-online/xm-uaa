package com.icthh.xm.uaa.service.permission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.domain.mapper.PermissionMapper;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static java.util.Optional.ofNullable;

/**
 * Created by victor on 22.06.2020.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class ConfigServiceConfigurationSource implements ConfigurationSource {
    private static final String EMPTY_YAML = "---";

    private static final String API = "/api";

    private static final String CUSTOM_PRIVILEGES_PATH = "/config/tenants/{tenantName}/custom-privileges.yml";

    @Value("${xm-permission.custom-privileges-path:}")
    private String customPrivilegesPath;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final PermissionProperties permissionProperties;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final CommonConfigRepository commonConfigRepository;

    @Override
    public PermissionsConfigMode getMode() {
        return PermissionsConfigMode.CONFIGURATION_SERVICE;
    }

    @Override
    public Map<String, Role> getRoles() {
        TreeMap<String, Role> roles = getConfig(permissionProperties.getRolesSpecPath(),
            new TypeReference<>() {
            });
        return roles != null ? roles : new TreeMap<>();
    }

    @Override
    public Map<String, Set<Privilege>> getPrivileges() {
        String privilegesFile = getCommonConfigContent(permissionProperties.getPrivilegesSpecPath()).orElse("");
        return StringUtils.isBlank(privilegesFile) ? new TreeMap<>() : PrivilegeMapper
            .ymlToPrivileges(privilegesFile);
    }

    @Override
    public Map<String, Map<String, Set<Permission>>> getPermissions() {
        SortedMap<String, SortedMap<String, SortedSet<Permission>>> permissions = getConfig(
            permissionProperties.getPermissionsSpecPath(),
            new TypeReference<>() {
            });
        permissions = permissions != null ? permissions : new TreeMap<>();
        Map<String, Map<String, Set<Permission>>> result = new TreeMap<>();
        permissions.forEach((key, value) -> result.put(key, value != null ? new TreeMap<>(value) : null));
        return result;
    }

    @SneakyThrows
    private <T> T getConfig(String configPath, TypeReference<T> typeReference) {
        String config = getConfigContent(configPath).orElse(EMPTY_YAML);
        return mapper.readValue(config, typeReference);
    }

    @Override
    public Map<String, Set<Privilege>> getCustomPrivileges() {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());
        String path = StringUtils.isBlank(customPrivilegesPath) ? CUSTOM_PRIVILEGES_PATH : customPrivilegesPath;
        String privilegesFile = getConfigContent(path.replace("{tenantName}", tenant)).orElse("");
        return StringUtils.isBlank(privilegesFile) ? new TreeMap<>() : PrivilegeMapper
            .ymlToPrivileges(privilegesFile);
    }

    @Override
    public void deletePermissionsForRemovedPrivileges(String msName, Collection<String> activePrivileges) {
        //nop - config server handles deletion by his own
    }

    @Override
    public Map<String, Permission> getRolePermissions(String roleKey) {
        String permissionsFile = getConfigContent(permissionProperties.getPermissionsSpecPath()).orElse("");

        if (StringUtils.isBlank(permissionsFile)) {
            return Collections.emptyMap();
        }

        return PermissionMapper.ymlToPermissions(permissionsFile);
    }

    @Override
    @SneakyThrows
    public void updateRoles(Map<String, Role> roles) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());

        tenantConfigRepository.updateConfigFullPath(tenant,
            API + permissionProperties.getRolesSpecPath(), mapper.writeValueAsString(roles));
    }

    @Override
    @SneakyThrows
    public void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());

        tenantConfigRepository.updateConfigFullPath(tenant, API + permissionProperties.getPermissionsSpecPath(),
            mapper.writeValueAsString(permissions));
    }

    private Optional<String> getConfigContent(String configPath) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());
        String config = null;
        try {
            config = tenantConfigRepository.getConfigFullPath(tenant, API + configPath);
            if (StringUtils.isBlank(config) || EMPTY_YAML.equals(config)) {
                config = null;
            }

        } catch (HttpClientErrorException e) {
            log.warn("Error while getting '{}'", configPath, e);
        }

        return ofNullable(config);
    }

    private Optional<String> getCommonConfigContent(String configPath) {
        return commonConfigRepository.getConfig(null, Collections.singletonList(configPath))
            .values().stream()
            .map(Configuration::getContent)
            .findFirst();
    }


}
