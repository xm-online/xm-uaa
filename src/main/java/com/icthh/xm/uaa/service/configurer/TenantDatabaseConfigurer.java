package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner;
import org.springframework.stereotype.Service;

@Service
public class TenantDatabaseConfigurer extends TenantManagerConfigurer {
    public TenantDatabaseConfigurer(TenantDatabaseProvisioner tenantProvisioner) {
        super(tenantProvisioner);
    }
}
