package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.lep.LepContext;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        UaaApp.class,
        XmOverrideConfiguration.class,
        LepContextCastIntTest.TestLepConfiguration.class
})
public class LepContextCastIntTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private TestLepService testLepService;

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
    @Transactional
    @SneakyThrows
    public void testLepContextCast() {
        String prefix = "/config/tenants/RESINTTEST/uaa/lep/test/";
        String key = prefix + "ScriptWithAround$$around.groovy";
        String body = "import com.icthh.xm.uaa.lep.LepContext;\nLepContext context = lepContext\nreturn ['context':context]";
        leps.onRefresh(key, body);
        Map<String, Object> result = testLepService.sayHello();
        assertTrue(result.get("context") instanceof LepContext);
        leps.onRefresh(key, null);
    }

    @Configuration
    public static class TestLepConfiguration {
        @Bean
        public TestLepService testLepService() {
            return new TestLepService();
        }
    }

    @LepService(group = "test")
    public static class TestLepService {
        @LogicExtensionPoint("ScriptWithAround")
        public Map<String, Object> sayHello() {
            return Map.of();
        }
    }


}
