package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.domain.mapper.PermissionMapper;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import com.icthh.xm.uaa.service.dto.RoleDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO.PermissionMatrixDTO;
import com.icthh.xm.uaa.service.mapper.PermissionDomainMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Service Implementation for managing Role.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRoleService {

    private static final String API = "/api";

    private static final String EMPTY_YAML = "---";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final PermissionProperties permissionProperties;
    private final TenantConfigRepository tenantConfigRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final EnvironmentService environmentService;

    /**
     * Get roles properties.
     * @return role props
     */
    public Map<String, Role> getRoles() {
        TreeMap<String, Role> roles = getConfig(permissionProperties.getRolesSpecPath(),
                        new TypeReference<TreeMap<String, Role>>() {});
        return roles != null ? roles : new TreeMap<>();
    }

    private Map<String, Map<String, Set<Permission>>> getPermissions() {
        Map<String, Map<String, Set<Permission>>> permissions = getConfig(
            permissionProperties.getPermissionsSpecPath(),
            new TypeReference<TreeMap<String, TreeMap<String, TreeSet<Permission>>>>() {
            });
        return permissions != null ? permissions : new TreeMap<>();
    }

    private Map<String, Set<Privilege>> getPrivileges() {
        String privilegesFile = getConfigContent(permissionProperties.getPrivilegesSpecPath()).orElse("");
        return StringUtils.isBlank(privilegesFile) ? new TreeMap<>() : PrivilegeMapper
                        .ymlToPrivileges(privilegesFile);
    }

    /**
     * Add role.
     * @param roleDto the role dto
     */
    @SneakyThrows
    public void addRole(RoleDTO roleDto) {
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

        updatePermissions(mapper.writeValueAsString(permissions));
    }

    /**
     * Update role.
     * @param roleDto the role dto
     */
    @SneakyThrows
    public void updateRole(RoleDTO roleDto) {
        Map<String, Role> roles = getRoles();
        Role role = roles.get(roleDto.getRoleKey());
        if (role == null) {
            throw new BusinessException("Role doesn't exist");
        }
        // role updating
        role.setDescription(roleDto.getDescription());
        role.setUpdatedBy(xmAuthenticationContextHolder.getContext().getRequiredLogin());
        role.setUpdatedDate(Instant.now().toString());
        roles.put(roleDto.getRoleKey(), role);
        updateRoles(mapper.writeValueAsString(roles));

        if (CollectionUtils.isEmpty(roleDto.getPermissions())) {
            return;
        }
        // permission updating
        Map<String, Map<String, Set<Permission>>> existingPermissions = getPermissions();
        enrichExistingPermissions(existingPermissions, roleDto.getPermissions());

        updatePermissions(mapper.writeValueAsString(existingPermissions));
    }

    @SneakyThrows
    public List<PermissionDTO> getRolePermissions(String roleKey) {
        String permissionsFile = getConfigContent(permissionProperties.getPermissionsSpecPath()).orElse("");

        if (StringUtils.isBlank(permissionsFile)) {
            return Collections.emptyList();
        }

        Map<String, Permission> permissions = PermissionMapper.ymlToPermissions(permissionsFile);

        return permissions.entrySet().stream()
            .map(Map.Entry::getValue)
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
    private void updatePermissions(String permissionsYml) {
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
        getPrivileges().forEach((msName, privileges) ->
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
                roleDto.getPermissions().add(permission);
            }));

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
        updatePermissions(mapper.writeValueAsString(permissions));

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
        getPrivileges().values().forEach(privileges ->
            roleMatrix.getPermissions().addAll(privileges.stream().map(privilege -> {
                PermissionMatrixDTO permission = matrixPermissions
                    .get(privilege.getMsName() + ":" + privilege.getKey());
                if (permission == null) {
                    permission = new PermissionMatrixDTO();
                    permission.setMsName(privilege.getMsName());
                    permission.setPrivilegeKey(privilege.getKey());
                }
                return permission;
            }).collect(Collectors.toList()))
        );

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
        updatePermissions(mapper.writeValueAsString(allPermissions));
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
                    Collection<PermissionDTO> newPermissions) {
        newPermissions.forEach(permissionDto -> {
            existingPermissions.putIfAbsent(permissionDto.getMsName(), new TreeMap<>());
            existingPermissions.get(permissionDto.getMsName()).putIfAbsent(permissionDto.getRoleKey(),
                            new TreeSet<>());
            Permission permission = PermissionDomainMapper.permissionDtoToPermission(permissionDto);
            existingPermissions.get(permissionDto.getMsName()).get(permissionDto.getRoleKey());
            // needed explicitly delete old permission
            existingPermissions.get(permissionDto.getMsName()).get(permissionDto.getRoleKey()).remove(permission);
            existingPermissions.get(permissionDto.getMsName()).get(permissionDto.getRoleKey()).add(permission);
        });
    }

    @SneakyThrows
    private <T> T getConfig(String configPath, TypeReference typeReference) {
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

        return Optional.ofNullable(config);
    }
}
