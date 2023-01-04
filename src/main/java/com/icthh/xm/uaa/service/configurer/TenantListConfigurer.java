package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import org.springframework.stereotype.Service;

@Service
public class TenantListConfigurer extends TenantManagerConfigurer {
    public TenantListConfigurer(TenantListProvisioner tenantProvisioner) {
        super(tenantProvisioner);
    }
}
