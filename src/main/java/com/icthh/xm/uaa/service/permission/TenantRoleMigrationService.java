package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Service that allows migrating roles and permission configuration from VCS to database.
 * Will be helpful when populating a blank database after enabling database persistence
 * at the first time.
 */
@Service
@AllArgsConstructor
@Transactional
public class TenantRoleMigrationService {

    private final DatabaseConfigurationSource databaseConfigurationSource;
    private final ConfigServiceConfigurationSource configServiceConfigurationSource;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Gets role and permissions from VCS source and puts the the database using {@code tenantKey}
     */
    public void migrate(String tenantKey) {
        tenantContextHolder.getPrivilegedContext()
            .execute(TenantContextUtils.buildTenant(tenantKey),
                () -> {
                    Map<String, Role> roles = configServiceConfigurationSource.getRoles();
                    roles.forEach((key, value) -> value.setKey(key));
                    databaseConfigurationSource.updateRoles(roles);

                    Map<String, Map<String, Set<Permission>>> permissions = configServiceConfigurationSource.getPermissions();

                    permissions.forEach((msName, rolePermissions) ->
                        rolePermissions.forEach((key, value) -> value.forEach(permission -> {
                            permission.setMsName(msName);
                            permission.setRoleKey(key);
                        })));

                    databaseConfigurationSource.updatePermissions(permissions);
                });
    }

}
