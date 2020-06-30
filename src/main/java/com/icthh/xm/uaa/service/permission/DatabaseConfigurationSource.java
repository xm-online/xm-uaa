package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.uaa.domain.PermissionEntity;
import com.icthh.xm.uaa.domain.RoleEntity;
import com.icthh.xm.uaa.repository.RoleRepository;
import com.icthh.xm.uaa.service.mapper.PermissionDomainMapper;
import com.icthh.xm.uaa.service.mapper.RoleDomainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.IdentityMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ConfigurationSource} implementation that uses a database to keep Roles and Permissions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@Order(1)
public class DatabaseConfigurationSource implements ConfigurationSource {
    private final RoleRepository roleRepository;

    @Override
    public PermissionsConfigMode getMode() {
        return PermissionsConfigMode.DATABASE;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Role> getRoles() {
        return roleRepository.findAll().stream()
            .map(RoleDomainMapper::entityToRole)
            .collect(Collectors.toMap(Role::getKey, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Set<Permission>>> getPermissions() {
        Map<String, Map<String, Set<Permission>>> result = new HashMap<>();

        for (RoleEntity role : roleRepository.findAll()) {
            String roleKey = role.getRoleKey();

            for (PermissionEntity permission : role.getPermissions()) {
                Map<String, Set<Permission>> roleToPermissionMap = result.computeIfAbsent(permission.getMsName(), s -> new HashMap<>());
                Set<Permission> permissions = roleToPermissionMap.computeIfAbsent(roleKey, s -> new HashSet<>());
                permissions.add(PermissionDomainMapper.permissionEntityToPermission(permission, roleKey));
            }
        }

        return result;
    }

    @Override
    public void updateRoles(Map<String, Role> roles) {
        //get from db
        List<RoleEntity> current = roleRepository.findAll();
        Map<String, RoleEntity> currentByKey = current.stream().collect(Collectors.toMap(RoleEntity::getRoleKey, Function.identity()));

        //add new
        HashMap<String, Role> newRoles = new HashMap<>(roles);
        newRoles.keySet().removeAll(currentByKey.keySet());
        roleRepository.saveAll(newRoles.entrySet().stream()
            .map(d -> RoleDomainMapper.roleToEntity(d.getValue(), new RoleEntity(), d.getKey()))
            .collect(Collectors.toList())
        );

        //update existing
        HashMap<String, RoleEntity> updated = new HashMap<>(currentByKey);
        updated.keySet().retainAll(roles.keySet());
        List<RoleEntity> updatedEntities = updated.entrySet().stream()
            .map(e -> RoleDomainMapper.roleToEntity(roles.get(e.getKey()), e.getValue(), e.getKey()))
            .collect(Collectors.toList());
        roleRepository.saveAll(updatedEntities);


        //delete removed
        HashMap<String, RoleEntity> removed = new HashMap<>(currentByKey);
        removed.keySet().removeAll(roles.keySet());
        roleRepository.deleteAll(removed.values());
    }

    @Override
    public void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions) {
        List<RoleEntity> currentRoles = roleRepository.findAll();
        Map<String, RoleEntity> currentByKey = currentRoles.stream().collect(Collectors.toMap(RoleEntity::getRoleKey, Function.identity()));

        IdentityMap permissionsToMsName = mapPermissionsToMsName(permissions);

        Map<String, Set<Permission>> rolesToPermissions = mapRolesToPermissions(permissions);

        for (Map.Entry<String, Set<Permission>> rolePermissionEntry : rolesToPermissions.entrySet()) {
            String roleKey = rolePermissionEntry.getKey();
            RoleEntity role = currentByKey.get(roleKey);

            if (role == null) {
                throw new IllegalSaslStateException("Role not found for " + roleKey);
            }

            //updating permissions
            Map<Pair<String, String>, PermissionEntity> currentPermissions = role.getPermissions().stream()
                .collect(Collectors.toMap(
                    p -> Pair.of(p.getPrivilegeKey(), p.getMsName()),
                    Function.identity()
                ));


            Map<Pair<String, String>, Permission> permissionsToSet = rolePermissionEntry.getValue().stream()
                .collect(Collectors.toMap(
                    p -> Pair.of(p.getPrivilegeKey(), (String) permissionsToMsName.get(p)),
                    Function.identity()
                ));

            //add new
            HashMap<Pair<String, String>, Permission> newPermissions = new HashMap<>(permissionsToSet);
            newPermissions.keySet().removeAll(currentPermissions.keySet());

            role.getPermissions().addAll(
                newPermissions.values().stream()
                    .map(p -> PermissionDomainMapper.permissionToPermissionEntity(p, new PermissionEntity(), role, (String) permissionsToMsName.get(p)))
                    .collect(Collectors.toSet())
            );

            //update existing
            HashMap<Pair<String, String>, PermissionEntity> updatedPermissions = new HashMap<>(currentPermissions);
            updatedPermissions.keySet().retainAll(permissionsToSet.keySet());

            role.getPermissions().addAll(
                updatedPermissions.entrySet().stream()
                    .map(p -> PermissionDomainMapper.permissionToPermissionEntity(permissionsToSet.get(p.getKey()), p.getValue(), role, p.getKey().getRight()))
                    .collect(Collectors.toSet())
            );

            //remote deleted
            HashMap<Pair<String, String>, PermissionEntity> deletedPermissions = new HashMap<>(currentPermissions);
            deletedPermissions.keySet().removeAll(newPermissions.keySet());
            deletedPermissions.keySet().removeAll(updatedPermissions.keySet());

            role.getPermissions().removeIf(p -> deletedPermissions.containsKey(Pair.of(p.getPrivilegeKey(), p.getMsName())));
        }

        roleRepository.saveAll(currentRoles);
    }

    @Override
    public void deletePermissionsForRemovedPrivileges(String msName, Collection<String> activePrivileges) {
        log.info("Deleting permissions for ms {}", msName);

        if (!activePrivileges.isEmpty()) {
            roleRepository.deletePermissionsNotIn(msName, activePrivileges);
        } else {
            roleRepository.deletePermissionsByMsName(msName);
        }
    }

    @Override
    public Map<String, Permission> getRolePermissions(String roleKey) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Map<String, Set<Privilege>> getPrivileges() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<Privilege>> getCustomPrivileges() {
        throw new UnsupportedOperationException();
    }

    private Map<String, Set<Permission>> mapRolesToPermissions(Map<String, Map<String, Set<Permission>>> permissions) {
        return permissions.values().stream()
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> {
                    HashSet<Permission> result = new HashSet<>(left);
                    result.addAll(right);
                    return result;
                }
            ));
    }

    private IdentityMap mapPermissionsToMsName(Map<String, Map<String, Set<Permission>>> permissions) {
        IdentityMap permissionsToMs = new IdentityMap();

        for (Map.Entry<String, Map<String, Set<Permission>>> stringMapEntry : permissions.entrySet()) {
            String msName = stringMapEntry.getKey();
            for (Set<Permission> pSet : stringMapEntry.getValue().values()) {
                pSet.forEach(perm -> permissionsToMs.put(perm, msName));
            }

        }
        return permissionsToMs;
    }

}
