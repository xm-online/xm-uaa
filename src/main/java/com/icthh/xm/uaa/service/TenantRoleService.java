package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import com.icthh.xm.commons.permission.service.PermissionMappingService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import com.icthh.xm.uaa.service.dto.RoleDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO.PermissionMatrixDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.validation.Valid;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.permission.domain.ReactionStrategy.EXCEPTION;
import static com.icthh.xm.uaa.service.dto.PermissionType.SYSTEM;
import static com.icthh.xm.uaa.service.dto.PermissionType.TENANT;
import static com.icthh.xm.uaa.service.mapper.PermissionDomainMapper.permissionDtoToPermission;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_FORBIDDEN_ROLE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

/**
 * Service Implementation for managing Role.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRoleService {

    private static final String API = "/api";

    private static final String EMPTY_YAML = "---";

    private static final String CUSTOM_PRIVILEGES_PATH = "/config/tenants/{tenantName}/custom-privileges.yml";

    @Value("${xm-permission.custom-privileges-path:}")
    private String customPrivilegesPath;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final PermissionProperties permissionProperties;
    private final TenantConfigRepository tenantConfigRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final EnvironmentService environmentService;
    private final CommonConfigRepository commonConfigRepository;
    private final TenantPropertiesService tenantPropertiesService;
    private final PermissionMappingService permissionMappingService;

    /**
     * Get roles properties.
     * @return role props
     */
    public Map<String, Role> getRoles() {
        TreeMap<String, Role> roles = getConfig(permissionProperties.getRolesSpecPath(),
            new TypeReference<>() {
            });
        return roles != null ? roles : new TreeMap<>();
    }

    private Map<String, Map<String, Set<Permission>>> getPermissions() {
        SortedMap<String, SortedMap<String, SortedSet<Permission>>> permissions = getConfig(
            permissionProperties.getPermissionsSpecPath(),
            new TypeReference<>() {
            });
        permissions = permissions != null ? permissions : new TreeMap<>();
        Map<String, Map<String, Set<Permission>>> result = new TreeMap<>();
        permissions.forEach((key, value) -> result.put(key, value != null ? new TreeMap<>(value) : null));
        return result;
    }

    private Map<String, Set<Privilege>> getPrivileges() {
        String privilegesFile = getCommonConfigContent(permissionProperties.getPrivilegesSpecPath()).orElse("");
        return StringUtils.isBlank(privilegesFile) ? new TreeMap<>() : PrivilegeMapper
                        .ymlToPrivileges(privilegesFile);
    }

    private Map<String, Set<Privilege>> getCustomPrivileges() {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());
        String path = StringUtils.isBlank(customPrivilegesPath) ? CUSTOM_PRIVILEGES_PATH : customPrivilegesPath;
        String privilegesFile = getConfigContent(path.replace("{tenantName}", tenant)).orElse("");
        return StringUtils.isBlank(privilegesFile) ? new TreeMap<>() : PrivilegeMapper
            .ymlToPrivileges(privilegesFile);
    }

    /**
     * Add role.
     * @param roleDto the role dto
     */
    @SneakyThrows
    public void addRole(@Valid RoleDTO roleDto) {

        if (StringUtils.equalsIgnoreCase(RoleConstant.SUPER_ADMIN, roleDto.getRoleKey())) {
            throw new BusinessException(ERROR_FORBIDDEN_ROLE, "Forbidden role key");
        }

        Map<String, Role> roles = getRoles();

        if (null != roles.get(roleDto.getRoleKey())) {
            throw new BusinessException("Role already exists");
        }

        Role role = new Role();
        role.setDescription(roleDto.getDescription());
        role.setCreatedBy(xmAuthenticationContextHolder.getContext().getRequiredLogin());
        role.setCreatedDate(Instant.now().toString());
        role.setUpdatedBy(xmAuthenticationContextHolder.getContext().getRequiredLogin());
        role.setUpdatedDate(roleDto.getUpdatedDate());
        roles.put(roleDto.getRoleKey(), role);

        updateRoles(mapper.writeValueAsString(roles));

        Map<String, Map<String, Set<Permission>>> permissions = getPermissions();
        if (StringUtils.isBlank(roleDto.getBasedOn())) {
            enrichExistingPermissions(permissions, roleDto.getRoleKey());
        } else {
            enrichExistingPermissions(permissions, roleDto.getRoleKey(), roleDto.getBasedOn());
        }

        updatePermissions(permissions);
    }

    private void removeDefaultValues(Map<String, Map<String, Set<Permission>>> permissions) {
        if (TRUE.equals(tenantPropertiesService.getTenantProps().getSecurity().getRemoveDefaultPermissions())) {
            permissions.values()
                .stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .forEach(role -> role.removeIf(this::isPermissionHasDefaultValue));
        }
    }

    private boolean isPermissionHasDefaultValue(Permission it) {
        return it.isDisabled()
            && it.getEnvCondition() == null
            && it.getResourceCondition() == null
            && (it.getReactionStrategy() == null || it.getReactionStrategy() == EXCEPTION);
    }

    /**
     * Update role.
     * @param roleDto the role dto
     */
    @SneakyThrows
    public void updateRole(RoleDTO roleDto) {
        Map<String, Role> roles = getRoles();
        Role roleToUpdate = roles.get(roleDto.getRoleKey());

        if (roleToUpdate == null) {
            throw new BusinessException("Role doesn't exist");
        }

        roleToUpdate.setDescription(roleDto.getDescription());
        roleToUpdate.setUpdatedBy(xmAuthenticationContextHolder.getContext().getRequiredLogin());
        roleToUpdate.setUpdatedDate(Instant.now().toString());

        roles.put(roleDto.getRoleKey(), roleToUpdate);

        updateRoles(mapper.writeValueAsString(roles));

        Collection<PermissionDTO> newPermissions = roleDto.getPermissions();

        if (newPermissions.isEmpty()) {
            return;
        }

        Map<String, Map<String, Set<Permission>>> existingPermissions = getPermissions();

        enrichExistingPermissions(existingPermissions, newPermissions);

        updatePermissions(existingPermissions);
    }

    @SneakyThrows
    public List<PermissionDTO> getRolePermissions(String roleKey) {
        String permissionsFile = getConfigContent(permissionProperties.getPermissionsSpecPath()).orElse("");

        if (StringUtils.isBlank(permissionsFile)) {
            return Collections.emptyList();
        }

        Map<String, Permission> permissions = permissionMappingService.ymlToPermissions(permissionsFile);

        return permissions.values()
                          .stream()
                          .filter(perm -> roleKey.equals(perm.getRoleKey()))
                          .map(PermissionDTO::new)
                          .collect(Collectors.toList());
    }

    /**
     * Update roles properties.
     * @param rolesYml roles config yml
     */
    @SneakyThrows
    private void updateRoles(String rolesYml) {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());

        tenantConfigRepository.updateConfigFullPath(tenant,
            API + permissionProperties.getRolesSpecPath(), rolesYml);
    }

    @SneakyThrows
    private void updatePermissions(Map<String, Map<String, Set<Permission>>> permission) {
        removeDefaultValues(permission);
        String permissionsYml = mapper.writeValueAsString(permission);
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());

        tenantConfigRepository.updateConfigFullPath(tenant, API + permissionProperties.getPermissionsSpecPath(),
            permissionsYml);
    }

    /**
     * Get all roles.
     * @return roles set
     */
    public Set<RoleDTO> getAllRoles() {
        return getRoles().entrySet().stream()
            .peek(entry -> entry.getValue().setKey(entry.getKey()))
            .map(Map.Entry::getValue)
            .map(RoleDTO::new)
            .collect(Collectors.toSet());
    }

    /**
     * Get role with permissions by role key.
     * @param roleKey the role key
     * @return roleDTO
     */
    public Optional<RoleDTO> getRole(String roleKey) {
        Role role = getRoles().get(roleKey);
        if (role == null) {
            return Optional.empty();
        }
        role.setKey(roleKey);
        RoleDTO roleDto = new RoleDTO(role);
        roleDto.setPermissions(new TreeSet<>());

        // map key = MS_NAME:PRIVILEGE_KEY, value = PermissionDTO
        Map<String, PermissionDTO> permissions = new TreeMap<>();

        // create permissions dto with role permissions
        getPermissions().forEach((msName, rolePermissions) ->
            rolePermissions.entrySet().stream()
                .filter(entry -> roleKey.equalsIgnoreCase(entry.getKey()))
                .forEach(entry ->
                    entry.getValue().forEach(permission -> {
                        permission.setMsName(msName);
                        permission.setRoleKey(roleKey);
                        permissions.put(msName + ":" + permission.getPrivilegeKey(),
                            new PermissionDTO(permission));
                    })
                ));

        // enrich role permissions with missing privileges
        BiConsumer<String, Set<Privilege>> privilegesProcessor = (msName, privileges) ->
            privileges.forEach(privilege -> {
                PermissionDTO permission = permissions.get(msName + ":" + privilege.getKey());
                if (permission == null) {
                    permission = new PermissionDTO();
                    permission.setMsName(msName);
                    permission.setRoleKey(roleKey);
                    permission.setPrivilegeKey(privilege.getKey());
                    permission.setEnabled(false);
                }
                permission.setResources(privilege.getResources());
                permission.setDescription(privilege.getCustomDescription());
                roleDto.getPermissions().add(permission);
            });

        getPrivileges().forEach(privilegesProcessor);
        roleDto.getPermissions().forEach(it -> it.setPermissionType(SYSTEM));
        Map<String, Set<Privilege>> customPrivileges = getCustomPrivileges();
        customPrivileges.forEach(privilegesProcessor);
        Set<String> customPrivilegeKeys = customPrivileges.values().stream().flatMap(Set::stream).map(Privilege::getKey)
            .collect(Collectors.toSet());
        roleDto.getPermissions().stream()
            .filter(it -> customPrivilegeKeys.contains(it.getPrivilegeKey())).forEach(it -> {
            if (it.getPermissionType() == SYSTEM) {
                log.error("Custom privilege {} try to override system privilege, and ignored", it);
            } else {
                it.setPermissionType(TENANT);
            }
        });

        roleDto.setEnv(environmentService.getEnvironments());

        return Optional.of(roleDto);
    }

    @SneakyThrows
    public void deleteRole(String roleKey) {
        if (CollectionUtils.isNotEmpty(userRepository.findByRoleKey(roleKey))) {
            throw new BusinessException("Failed to delete role. Role is assigned to user.");
        }
        if (CollectionUtils.isNotEmpty(clientRepository.findByRoleKey(roleKey))) {
            throw new BusinessException("Failed to delete role. Role is assigned to client.");
        }

        Map<String, Role> roles = getRoles();
        roles.remove(roleKey);
        updateRoles(mapper.writeValueAsString(roles));

        Map<String, Map<String, Set<Permission>>> permissions = getPermissions();
        for (Map<String, Set<Permission>> perm : permissions.values()) {
            perm.remove(roleKey);
        }
        updatePermissions(permissions);

    }

    /**
     * Get role matrix.
     * @return the role matrix
     */
    public RoleMatrixDTO getRoleMatrix() {
        RoleMatrixDTO roleMatrix = new RoleMatrixDTO();
        roleMatrix.setRoles(getRoles().keySet());

        // map key = MS_NAME:PRIVILEGE_KEY, value = PermissionMatrixDTO
        Map<String, PermissionMatrixDTO> matrixPermissions = new HashMap<>();

        // create permissions matrix dto with role permissions
        getPermissions().forEach((msName, rolePermissions) ->
            rolePermissions.forEach((roleKey, permissions) ->
                    permissions.forEach(permission -> {
                        PermissionMatrixDTO permissionMatrix = matrixPermissions
                            .get(msName + ":" + permission.getPrivilegeKey());
                        if (permissionMatrix == null) {
                            permissionMatrix = new PermissionMatrixDTO();
                            permissionMatrix.setMsName(msName);
                            permissionMatrix.setPrivilegeKey(permission.getPrivilegeKey());
                            matrixPermissions.put(msName + ":" + permission.getPrivilegeKey(), permissionMatrix);
                        }
                        if (!permission.isDisabled()) {
                            permissionMatrix.getRoles().add(roleKey);
                        }
                    })
                ));

        // enrich role permissions with missing privileges
        Consumer<Set<Privilege>> privilegesProcessor = privileges ->
            roleMatrix.getPermissions().addAll(privileges.stream().map(privilege -> {
                PermissionMatrixDTO permission = matrixPermissions
                    .get(privilege.getMsName() + ":" + privilege.getKey());
                if (permission == null) {
                    permission = new PermissionMatrixDTO();
                    permission.setMsName(privilege.getMsName());
                    permission.setPrivilegeKey(privilege.getKey());
                }
                permission.setDescription(privilege.getCustomDescription());
                return permission;
            }).collect(Collectors.toList()));


        getPrivileges().values().forEach(privilegesProcessor);
        roleMatrix.getPermissions().forEach(it -> it.setPermissionType(SYSTEM));
        Map<String, Set<Privilege>> customPrivileges = getCustomPrivileges();
        customPrivileges.values().forEach(privilegesProcessor);
        Set<String> customPrivilegeKeys = customPrivileges.values().stream().flatMap(Set::stream).map(Privilege::getKey)
            .collect(Collectors.toSet());
        roleMatrix.getPermissions().stream()
            .filter(it -> customPrivilegeKeys.contains(it.getPrivilegeKey())).forEach(it -> {
            if (it.getPermissionType() == SYSTEM) {
                log.error("Custom privilege {} try to override system privilege, and ignored", it.getPrivilegeKey());
            } else {
                it.setPermissionType(TENANT);
            }
        });

        return roleMatrix;
    }

    /**
     * Update permissions by role matrix.
     * @param roleMatrix the role matrix
     */
    @SneakyThrows
    public void updateRoleMatrix(RoleMatrixDTO roleMatrix) {
        // create map key: MS_NAME:PRIVILEGE_KEY, value: PermissionMatrixDTO for easy search
        Map<String, PermissionMatrixDTO> newPermissions = new HashMap<>();
        for (PermissionMatrixDTO permission : roleMatrix.getPermissions()) {
            newPermissions.put(permission.getMsName() + ":" + permission.getPrivilegeKey(), permission);
        }

        Map<String, Map<String, Set<Permission>>> allPermissions = getPermissions();
        allPermissions.forEach((msName, rolePermissions) ->
            rolePermissions.entrySet().stream()
                // do not update hidden roles
                .filter(roleWithPermissions -> roleMatrix.getRoles().contains(roleWithPermissions.getKey()))
                // roleWithPermissions -> key: ROLE_KEY, value: set of role permissions
                .forEach(roleWithPermissions ->
                    roleWithPermissions.getValue().forEach(permission -> {
                        String key = msName + ":" + permission.getPrivilegeKey();
                        if (newPermissions.get(key) != null) {
                            /*
                             * disable permissions for current ROLE_KEY if it
                             * is not present in roleMatrix.permissions[].roles[] list
                             */
                            Set<String> roles = newPermissions.get(key).getRoles();
                            if (roles.contains(roleWithPermissions.getKey())) {
                                permission.setDisabled(false);
                                roles.remove(roleWithPermissions.getKey());
                            } else {
                                permission.setDisabled(true);
                            }
                        }
                    }))
        );

        // processing permissions for new role
        roleMatrix.getPermissions().stream().filter(permissionMatrixDTO ->
            !permissionMatrixDTO.getRoles().isEmpty()).forEach(permissionMatrixDTO -> {
                allPermissions.putIfAbsent(permissionMatrixDTO.getMsName(), new TreeMap<>());
                permissionMatrixDTO.getRoles().forEach(role -> {
                    allPermissions.get(permissionMatrixDTO.getMsName()).putIfAbsent(role, new TreeSet<>());
                    Permission permission = new Permission();
                    permission.setPrivilegeKey(permissionMatrixDTO.getPrivilegeKey());
                    permission.setDisabled(false);
                    allPermissions.get(permissionMatrixDTO.getMsName()).get(role).add(permission);
                });
            });

        updatePermissions(allPermissions);
    }

    /**
     * Set permissions based on role.
     * @param existingPermissions existing permissions
     * @param role the role that will be assigned with permissions
     * @param basedOn the role from where permissions will be received
     */
    private void enrichExistingPermissions(
                    Map<String, Map<String, Set<Permission>>> existingPermissions,
                    String role,
                    String basedOn) {
        for (Map<String, Set<Permission>> perm : existingPermissions.values()) {
            perm.put(role, perm.getOrDefault(basedOn, new TreeSet<>()));
        }
    }

    /**
     * Set permissions for role.
     * @param existingPermissions existing permissions
     * @param role the role that will be assigned with permissions
     */
    private void enrichExistingPermissions(
                    Map<String, Map<String, Set<Permission>>> existingPermissions,
                    String role) {
        for (Map<String, Set<Permission>> perm : existingPermissions.values()) {
            perm.put(role, new TreeSet<>());
        }
    }

    /**
     * Set permissions to role.
     * @param existingPermissions existing permissions
     * @param newPermissions permissions to add
     */
    private void enrichExistingPermissions(
        Map<String, Map<String, Set<Permission>>> existingPermissions,
        Collection<PermissionDTO> newPermissions
    ) {
        newPermissions.forEach(newPermission -> {
            String msName = newPermission.getMsName();
            String roleKey = newPermission.getRoleKey();
            existingPermissions.putIfAbsent(msName, new TreeMap<>());
            existingPermissions.get(msName).putIfAbsent(roleKey, new TreeSet<>());

            Permission permission = permissionDtoToPermission(newPermission);
            Set<Permission> rolePermissions = existingPermissions.get(msName).get(roleKey);
            // needed explicitly delete old permission
            rolePermissions.remove(permission);
            rolePermissions.add(permission);
        });
    }

    @SneakyThrows
    private <T> T getConfig(String configPath, TypeReference<T> typeReference) {
        String config = getConfigContent(configPath).orElse(EMPTY_YAML);
        return mapper.readValue(config, typeReference);
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
