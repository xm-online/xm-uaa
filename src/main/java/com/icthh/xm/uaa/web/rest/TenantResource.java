package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResource implements TenantsApiDelegate {

    private final TenantManager tenantManager;

    @Override
    @Transactional
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'UAA.TENANT.CREATE')")
    @PrivilegeDescription("Privilege to add a new uaa tenant")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantManager.createTenant(tenant);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'UAA.TENANT.DELETE')")
    @PrivilegeDescription("Privilege to delete uaa tenant by tenantKey")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantManager.deleteTenant(tenantKey);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission(null, 'UAA.TENANT.GET_LIST')")
    @PrivilegeDescription("Privilege to get all uaa tenants")
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'UAA.TENANT.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get uaa tenant")
    public ResponseEntity<Tenant> getTenant(String s) {
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'status':#status}, 'UAA.TENANT.UPDATE')")
    @PrivilegeDescription("Privilege to update uaa tenant")
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantManager.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
