package com.icthh.xm.uaa.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.mapper.PermissionMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.service.dto.AccPermissionDTO;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for retrieving Permissions from refreshable configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPermissionService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final PermissionProperties permissionProperties;
    private final TenantContextHolder tenantContextHolder;

    private final AntPathMatcher matcher = new AntPathMatcher();

    // Map structure: <Tenant, <Role, [Permission]>>
    final Map<String, Map<String, List<Permission>>> tenantRolePermissions = new ConcurrentHashMap<>();

    public List<AccPermissionDTO> getEnabledPermissionByRole(List<String> roles) {

        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());

        return ofNullable(roles).orElse(emptyList()).stream().map(role ->
            tenantRolePermissions.getOrDefault(tenant, Collections.emptyMap())
                .getOrDefault(role, emptyList())
                .stream()
                .filter(permission -> !permission.isDisabled())
                .map(AccPermissionDTO::new)
                .collect(Collectors.toList()))
            .filter(CollectionUtils::isNotEmpty)
            .findFirst()
            .orElse(emptyList());
    }

    @Override
    public void onRefresh(final String updatedKey, final String config) {
        String specificationPathPattern = permissionProperties.getPermissionsSpecPath();
        try {

            String tenant = matcher.extractUriTemplateVariables(specificationPathPattern, updatedKey).get(TENANT_NAME);

            if (StringUtils.isBlank(config)) {
                tenantRolePermissions.remove(tenant);
                log.info("Permission configuration was removed for tenant [{}] by key [{}]", tenant, updatedKey);
            } else {
                Map<String, Permission> permissions = PermissionMapper.ymlToPermissions(config);

                Map<String, List<Permission>> tenantPermissions = new HashMap<>();

                permissions.values()
                           .forEach(p -> tenantPermissions.computeIfAbsent(p.getRoleKey(), role -> new LinkedList<>())
                                                          .add(p));

                tenantRolePermissions.put(tenant, Collections.unmodifiableMap(tenantPermissions));

                log.info("Permission configuration was updated for tenant [{}] by key [{}]", tenant, updatedKey);
            }
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(final String updatedKey) {
        return matcher.match(permissionProperties.getPermissionsSpecPath(), updatedKey);
    }

    @Override
    public void onInit(final String configKey, final String configValue) {
        onRefresh(configKey, configValue);
    }
}
