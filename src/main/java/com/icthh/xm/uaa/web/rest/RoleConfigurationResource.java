package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.uaa.service.DatabaseConfigurationSource;
import com.icthh.xm.uaa.service.TenantRoleMigrationService;
import com.icthh.xm.uaa.service.TenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 *
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnBean(DatabaseConfigurationSource.class)
public class RoleConfigurationResource {

    private final TenantRoleMigrationService tenantRoleMigrationService;
    private final TenantRoleService tenantRoleService;

    /**
     * API that allows migrating roles and permission configuration from VCS to database.
     * Will be helpful when populating a blank database after enabling database persistence
     * at the first time.
     */
    @PostMapping("/roles/migrate")
    public void migrate(@PathVariable("tenant") String tenant) {
        tenantRoleMigrationService.migrate(tenant);
    }

    @GetMapping("/roles/{tenant}/configuration")
    public String getRoleConfiguration(@PathVariable("tenant") String tenantKey) {
        return tenantRoleService.getRoleConfiguration(tenantKey);
    }

    @GetMapping("/permissions/{tenant}/configuration")
    public String getPermissionConfiguration(@PathVariable("tenant") String tenantKey) {
        return tenantRoleService.getPermissionConfiguration(tenantKey);
    }
}
