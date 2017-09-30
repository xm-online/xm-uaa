package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.uaa.service.tenant.TenantService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantResource implements TenantsApiDelegate {

    private final TenantService tenantService;

    @Override
    @Transactional
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantService.createTenant(tenant.getTenantKey().toUpperCase());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteTenant(String tenant) {
        tenantService.deleteTenant(tenant.toUpperCase());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Tenant> getTenant(String s) {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> manageTenant(String tenant, String state) {
        tenantService.manageTenant(tenant, state);
        return ResponseEntity.ok().build();
    }
}
