package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantProvisioner;

public abstract class TenantManagerConfigurer {

    protected final TenantProvisioner tenantProvisioner;

    public TenantManagerConfigurer(TenantProvisioner tenantProvisioner) {
        this.tenantProvisioner = tenantProvisioner;
    }

    public void configure(TenantManager.TenantManagerBuilder builder) {
        builder.service(tenantProvisioner);
    }
}
