package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Describes a source that provides operations to deal with Roles, Permissions and Privileges.
 * Tenant-specific values are obtain using tenant context.
 */
public interface ConfigurationSource {

    /**
     * @return configuration mode the source is using
     */
    PermissionsConfigMode getMode();

    /**
     * @return Roles for a tenant. Key - role key, value - role itself.
     */
    Map<String, Role> getRoles();

    /**
     * @return Permissions for a tenant. Key - application name,
     * inner map key - role key, value - permission
     */
    Map<String, Map<String, Set<Permission>>> getPermissions();

    /**
     * Update roles for a tenant. This is a full state update.
     *
     * @param roles role configuration, Key - role key, value - role itself.
     */
    void updateRoles(Map<String, Role> roles);

    /**
     * Updates permissions for a tenant. This is a full state update.
     *
     * @param permissions permission configuration, where key - application name,
     * inner map key - role key, value - permission.
     */
    void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions);

    /**
     * @return permissions for a specific role by {@code roleKey}.
     */
    Map<String, Permission> getRolePermissions(String roleKey);

    /**
     * @return application privileges. Key - application name, value - set of privileges.
     */
    Map<String, Set<Privilege>> getPrivileges();

    /**
     * @return custom privileges for a tenant. Key - application name, value - set of privileges.
     */
    Map<String, Set<Privilege>> getCustomPrivileges();

    /**
     * Deletes permissions for not active privileges (i.e. not in {@code activePrivileges}
     *
     * @param msName application name
     * @param activePrivileges currently active privileges
     */
    void deletePermissionsForRemovedPrivileges(String msName, Collection<String> activePrivileges);
}
