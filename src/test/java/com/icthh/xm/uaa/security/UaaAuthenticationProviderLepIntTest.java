package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class UaaAuthenticationProviderLepIntTest {

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

    @Autowired
    private UaaAuthenticationProvider uaaAuthenticationProvider;

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
    public void testAuthenticatedLepWithProfileHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("profile", "xm");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String path = "/config/tenants/RESINTTEST/uaa/lep/security/provider/Authenticate$$xm$$around.groovy";
        lepLoader.onRefresh(path, "return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken('testUser', null)");
        Authentication authentication = uaaAuthenticationProvider.authenticate(null);
        assertEquals("Lep Authenticate$$xm$$around.groovy was not invoked", "testUser", authentication.getPrincipal());
        lepLoader.onRefresh(path, null);
    }

    @Test
    public void testAuthenticatedLepWithoutProfileHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        String path = "/config/tenants/RESINTTEST/uaa/lep/security/provider/Authenticate$$around.groovy";
        lepLoader.onRefresh(path, "return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken('testUser', null)");
        Authentication authentication = uaaAuthenticationProvider.authenticate(null);
        assertEquals("Lep Authenticate$$around.groovy was not invoked", "testUser", authentication.getPrincipal());
        lepLoader.onRefresh(path, null);
    }
}
