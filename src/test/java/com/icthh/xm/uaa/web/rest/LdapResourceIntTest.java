package com.icthh.xm.uaa.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.LepTextConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import java.nio.charset.Charset;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    LepTextConfiguration.class,
    UaaApp.class,
    XmOverrideConfiguration.class
})
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class LdapResourceIntTest {

    private static final String TENANT = "XM";
    private static final String BASE_DN = "dc=xm,dc=com";
    private static final String TEST_LDIF = "config/test.ldif";
    private static final int LDAP_SERVER_PORT = 1389;
    private static final String LDAP_SEARCH_TEMPLATE = "test_template";

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LdapResource ldapResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    private MockMvc restMockMvc;

    @Value("${:classpath:config/tenants/XM/uaa/uaa.yml}")
    private Resource spec;

    private TenantProperties tenantProperties;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
        .newInstance()
        .usingDomainDsn(BASE_DN)
        .importingLdifs(TEST_LDIF)
        .bindingToPort(LDAP_SERVER_PORT)
        .build();

    @Before
    public void setUp() throws Exception {
        restMockMvc = MockMvcBuilders.standaloneSetup(ldapResource)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();

        TenantContextUtils.setTenant(tenantContextHolder, TENANT);

        String conf = StreamUtils.copyToString(spec.getInputStream(), Charset.defaultCharset());
        tenantProperties = mapper.readValue(conf, TenantProperties.class);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @Test
    @SneakyThrows
    public void searchTemplateBadRequestTest() {
        restMockMvc.perform(get("/api/ldap/_search-with-template")).andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    public void searchTemplateTest() {
        restMockMvc.perform(get("/api/ldap/_search-with-template?templateKey="+ LDAP_SEARCH_TEMPLATE
            +"&templateParams=test"))
            .andExpect(status().isOk());
    }

}
