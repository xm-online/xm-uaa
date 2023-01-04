package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import org.springframework.stereotype.Service;

@Service
public class TenantAbilityCheckerConfigurer extends TenantManagerConfigurer {
    public TenantAbilityCheckerConfigurer(TenantAbilityCheckerProvisioner tenantProvisioner) {
        super(tenantProvisioner);
    }
}
