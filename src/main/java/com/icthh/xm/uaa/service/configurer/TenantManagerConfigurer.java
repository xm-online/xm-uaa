package com.icthh.xm.uaa.service.configurer;

import com.icthh.xm.commons.tenantendpoint.TenantManager;

public interface TenantManagerConfigurer {
    void configure(TenantManager.TenantManagerBuilder builder);
}
