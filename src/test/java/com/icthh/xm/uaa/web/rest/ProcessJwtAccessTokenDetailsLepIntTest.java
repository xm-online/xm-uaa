package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenDetailsPostProcessor;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the TenantPropertiesResourceIntTest REST controller.
 *
 * @see ProcessJwtAccessTokenDetailsLepIntTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class ProcessJwtAccessTokenDetailsLepIntTest {

    @Autowired
    private XmLepScriptConfigServerResourceLoader lepLoader;

    @Autowired
    private DomainJwtAccessTokenDetailsPostProcessor processor;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @SneakyThrows
    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void lepServiceGroupExplicitlyDefinedTest() {
        String path = "/config/tenants/RESINTTEST/uaa/lep/security/ProcessJwtAccessTokenDetails$$around.groovy";
        lepLoader.onRefresh(path,"lepContext.inArgs.details.put('wasCalled', true)");
        Map<String, Object> details = new HashMap<>();
        processor.processJwtAccessTokenDetails(null, details);
        assertEquals("Lep ProcessJwtAccessTokenDetails$$around was not invoked", Boolean.TRUE, details.get("wasCalled"));
        lepLoader.onRefresh(path,null);
    }

    @Test
    public void testProcessJwtTokenDetailsWithDefinedProfileHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("profile", "xm");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String path = "/config/tenants/RESINTTEST/uaa/lep/security/ProcessJwtAccessTokenDetails$$xm$$around.groovy";
        lepLoader.onRefresh(path,"lepContext.inArgs.details.put('profileWasCalled', true)");
        Map<String, Object> details = new HashMap<>();
        processor.processJwtAccessTokenDetails(null, details);
        assertEquals("Lep ProcessJwtAccessTokenDetails$$xm$$around.groovy was not invoked", Boolean.TRUE, details.get("profileWasCalled"));
        lepLoader.onRefresh(path,null);
    }

}
