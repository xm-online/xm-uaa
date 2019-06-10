package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.uaa.service.tenant.TenantService;
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

    private final TenantService tenantService;

    @Override
    @Transactional
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'UAA.TENANT.CREATE')")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantService.createTenant(tenant.getTenantKey());
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'UAA.TENANT.DELETE')")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantService.deleteTenant(tenantKey.toUpperCase());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission(null, 'UAA.TENANT.GET_LIST')")
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        return ResponseEntity.ok().build();
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'UAA.TENANT.GET_LIST.ITEM')")
    public ResponseEntity<Tenant> getTenant(String s) {
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'status':#status}, 'UAA.TENANT.UPDATE')")
    public ResponseEntity<Void> manageTenant(String tenant, String status) {
        tenantService.manageTenant(tenant, status);
        return ResponseEntity.ok().build();
    }
}
