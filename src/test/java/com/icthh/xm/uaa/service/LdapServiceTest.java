package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.TemplateParams;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import java.nio.charset.Charset;
import java.util.*;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Slf4j
public class LdapServiceTest {

    private static final String TENANT = "XM";
    private static final String BASE_DN = "dc=xm,dc=com";
    private static final String TEST_LDIF = "config/test.ldif";
    private static final int LDAP_SERVER_PORT = 1389;
    private static final String LDAP_SEARCH_TEMPLATE = "test_template";

    @Autowired
    private LdapService ldapService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

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
    public void searchByTemplateTest() {
        TemplateParams templateParams = getTemplateParams("test");

        Set<Map<String, List<String>>> result = ldapService.searchByTemplate(LDAP_SEARCH_TEMPLATE, templateParams);
        assertTrue(result.size() > 0);
        assertEquals(1, result.size());

        assertUserUid(result, "test");
    }

    @Test
    public void searchByTemplateSystemUserTest() {
        TemplateParams templateParams = getTemplateParams("system");

        Set<Map<String, List<String>>> result = ldapService.searchByTemplate(LDAP_SEARCH_TEMPLATE, templateParams);
        assertTrue(result.size() > 0);
        assertEquals(1, result.size());

        assertUserUid(result, "system");
    }

    @Test
    public void searchByTemplateWrongUserTest() {
        TemplateParams templateParams = getTemplateParams("youCantFindMe");

        Set<Map<String, List<String>>> result = ldapService.searchByTemplate(LDAP_SEARCH_TEMPLATE, templateParams);
        assertEquals(0, result.size());
    }

    private TemplateParams getTemplateParams(String parameter) {
        List<Object> params = new ArrayList<>();
        params.add(parameter);

        return new TemplateParams(params);
    }

    private void assertUserUid(Set<Map<String, List<String>>> source, String userUid) {
        Optional<String> user = Optional.empty();
        for (Map<String, List<String>> it : source) {
            for (Map.Entry<String, List<String>> entry : it.entrySet()) {
                Optional<String> result = entry.getValue().stream().filter(userUid::equals).findFirst();
                if (result.isPresent()) {
                    user = result;
                }
            }
        }
        assertTrue(user.isPresent());
    }

}
