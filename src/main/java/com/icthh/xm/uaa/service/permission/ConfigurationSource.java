package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.Role;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by victor on 22.06.2020. //todo V: add doc
 */
public interface ConfigurationSource {

    PermissionsConfigMode getMode();

    Map<String, Role> getRoles();

    /**
     * @return key - application name, inner map key - role key, value - permission
     */
    Map<String, Map<String, Set<Permission>>> getPermissions();

    void updateRoles(Map<String, Role> roles);

    void updatePermissions(Map<String, Map<String, Set<Permission>>> permissions);

    Map<String, Permission> getRolePermissions(String roleKey);

    Map<String, Set<Privilege>> getPrivileges();

    Map<String, Set<Privilege>> getCustomPrivileges();

    void deletePermissionsForRemovedPrivileges(String msName, Collection<String> data);
}
