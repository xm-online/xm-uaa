package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_SUPER_ADMIN_FORBIDDEN_OPERATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings.PasswordSettings;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test class for the TenantPropertiesResourceIntTest REST controller.
 *
 * @see TenantPropertiesResourceIntTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Ignore
public class TenantPropertiesResourceIntTest {


    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc restMvc;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);
    }

    @Before
    @SneakyThrows
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        this.restMvc = MockMvcBuilders.standaloneSetup(new TenantPropertiesResource(tenantPropertiesService))
                                      .setControllerAdvice(exceptionTranslator)
                                      .build();


    }

    @After
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

//    @Test
//    public void testNonAuthenticatedUser() throws Exception {
//
//        restMvc.perform(get("/api/uaa/properties/settings-public"))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.passwordSettings.maxLength").value(ERROR_SUPER_ADMIN_FORBIDDEN_OPERATION));
//
//    }


}
