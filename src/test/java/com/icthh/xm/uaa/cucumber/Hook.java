package com.icthh.xm.uaa.cucumber;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link Hook} class.
 */
public class Hook {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    public Hook() {
        super();
    }

    @cucumber.api.java.Before
    public void cucumberBefore() {
        tenantContextHolder.getPrivilegedContext().setTenant(buildTenant(DEFAULT_TENANT_KEY_VALUE));
    }

    @cucumber.api.java.After
    public void cucumberAfter() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

}
