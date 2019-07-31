package com.icthh.xm.uaa.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
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
public class TenantPropertiesResourceIntTest {

    private static final String PASSWORD_MAX_LENGTH = "5";
    private static final String PASSWORD_MIN_LENGTH = "15";
    private static final String PASSWORD_PATTERN = "(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}";
    private static final String PATTERN_MESSAGE = "Pattern Message";

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc restMvc;

    @Before
    @SneakyThrows
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        TenantProperties properties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setMaxLength(Byte.valueOf(PASSWORD_MAX_LENGTH));
        passwordSettings.setMinLength(Byte.valueOf(PASSWORD_MIN_LENGTH));
        passwordSettings.setPattern(PASSWORD_PATTERN);
        passwordSettings.setPatternMessage(of("en", PATTERN_MESSAGE));
        publicSettings.setPasswordSettings(passwordSettings);
        properties.setPublicSettings(publicSettings);

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        this.restMvc = MockMvcBuilders.standaloneSetup(new TenantPropertiesResource(tenantPropertiesService))
                                      .setControllerAdvice(exceptionTranslator)
                                      .build();
    }

    @Test
    public void testGetUaaPublicSettings() throws Exception {

        restMvc.perform(get("/api/uaa/properties/settings-public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordSettings.maxLength").value(PASSWORD_MAX_LENGTH))
            .andExpect(jsonPath("$.passwordSettings.minLength").value(PASSWORD_MIN_LENGTH))
            .andExpect(jsonPath("$.passwordSettings.pattern").value(PASSWORD_PATTERN))
            .andExpect(jsonPath("$.passwordSettings.patternMessage.en").value(PATTERN_MESSAGE));
    }

}
