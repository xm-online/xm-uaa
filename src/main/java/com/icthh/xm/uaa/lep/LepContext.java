package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import org.springframework.web.client.RestTemplate;

public class LepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    public Object methodResult;

    public LepServices services;
    public LepTemplates templates;

    public static class LepServices {

    }

    public static class LepTemplates {
        public RestTemplate rest;
    }

}
