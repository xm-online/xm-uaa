package com.icthh.xm.uaa.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.UserSpec;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings.PasswordPolicy;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private static final Long PASSWORD_POLICIES_MINIMAL_MATCH_COUNT = 1L;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc restMvc;

    @Before
    @SneakyThrows
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        TenantProperties properties = new TenantProperties();
        PublicSettings publicSettings = new PublicSettings();
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setMaxLength(Byte.parseByte(PASSWORD_MAX_LENGTH));
        passwordSettings.setMinLength(Byte.parseByte(PASSWORD_MIN_LENGTH));
        passwordSettings.setPattern(PASSWORD_PATTERN);
        passwordSettings.setPatternMessage(of("en", PATTERN_MESSAGE));
        passwordPolicy.setPattern(PASSWORD_PATTERN);
        passwordPolicy.setPatternMessage(of("en", PATTERN_MESSAGE));
        publicSettings.setPasswordPolicies(List.of(passwordPolicy));
        publicSettings.setPasswordSettings(passwordSettings);
        publicSettings.setPasswordPoliciesMinimalMatchCount(PASSWORD_POLICIES_MINIMAL_MATCH_COUNT);
        properties.setPublicSettings(publicSettings);

        properties.setUserSpec(List.of(
                new UserSpec("ROLE_1", "someDataSpec", "someDataForm"),
                new UserSpec("ROLE_2", "someDataSpec2", "someDataForm2")
        ));

        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        this.restMvc = MockMvcBuilders.standaloneSetup(new TenantPropertiesResource(tenantPropertiesService))
            .setControllerAdvice(exceptionTranslator)
            .build();
    }

    @Test
    public void testGetUaaPublicSettings() throws Exception {
        // to apply all filters and configurations
        this.restMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        restMvc.perform(get("/api/uaa/properties/settings-public"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordSettings.maxLength").value(PASSWORD_MAX_LENGTH))
            .andExpect(jsonPath("$.passwordSettings.minLength").value(PASSWORD_MIN_LENGTH))
            .andExpect(jsonPath("$.passwordSettings.pattern").value(PASSWORD_PATTERN))
            .andExpect(jsonPath("$.passwordSettings.patternMessage.en").value(PATTERN_MESSAGE))
            .andExpect(jsonPath("$.passwordPolicies[0].pattern").value(PASSWORD_PATTERN))
            .andExpect(jsonPath("$.passwordPoliciesMinimalMatchCount").value(PASSWORD_POLICIES_MINIMAL_MATCH_COUNT))
            .andExpect(jsonPath("$.passwordPolicies[0].patternMessage.en").value(PATTERN_MESSAGE))
            .andExpect(header().doesNotExist("Set-Cookie"))
            .andExpect(result -> assertNull(result.getRequest().getSession(false)))
        ;

    }

    @Test
    public void testGetUserSpec() throws Exception {

        restMvc.perform(get("/api/uaa/properties/data-schema/ROLE_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleKey").value("ROLE_1"))
                .andExpect(jsonPath("$.dataSpec").value("someDataSpec"))
                .andExpect(jsonPath("$.dataForm").value("someDataForm"));

        restMvc.perform(get("/api/uaa/properties/data-schema/ROLE_2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleKey").value("ROLE_2"))
                .andExpect(jsonPath("$.dataSpec").value("someDataSpec2"))
                .andExpect(jsonPath("$.dataForm").value("someDataForm2"));

        restMvc.perform(get("/api/uaa/properties/data-schema/ROLE_3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleKey").value("ROLE_3"))
                .andExpect(jsonPath("$.dataSpec").doesNotExist())
                .andExpect(jsonPath("$.dataForm").doesNotExist());

    }

    @Test
    @SneakyThrows
    public void testNullUserSpec() {
        tenantPropertiesService.onRefresh("/config/tenants/" + DEFAULT_TENANT_KEY_VALUE + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(new TenantProperties()));

        restMvc.perform(get("/api/uaa/properties/data-schema/ROLE_1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleKey").value("ROLE_1"))
            .andExpect(jsonPath("$.dataSpec").doesNotExist())
            .andExpect(jsonPath("$.dataForm").doesNotExist());
    }

}
