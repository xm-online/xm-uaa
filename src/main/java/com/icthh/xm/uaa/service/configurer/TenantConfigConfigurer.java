package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import org.springframework.stereotype.Service;

@Service
public class TenantConfigConfigurer extends TenantManagerConfigurer {
    public TenantConfigConfigurer(TenantConfigProvisioner tenantProvisioner) {
        super(tenantProvisioner);
    }
}
