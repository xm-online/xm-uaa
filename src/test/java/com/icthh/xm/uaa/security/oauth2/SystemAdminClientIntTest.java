package com.icthh.xm.uaa.security.oauth2;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.util.Base64;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, XmOverrideConfiguration.class}, properties = {
    "application.system-admin-client-id=system-admin",
    "application.system-admin-client-secret=systemAdminSecret",
    "management.endpoints.web.base-path=/management",
    "management.endpoints.web.exposure.include=env,health,prometheus"
})
public class SystemAdminClientIntTest {

    private static final String TOKEN_URL = "/oauth/token";
    private static final String TENANT_HEADER = "x-tenant";
    private static final String DEFAULT_CLIENT = "testClient";
    private static final String DEFAULT_CLIENT_SECRET = "clientSecret";
    private static final String SYSTEM_ADMIN_CLIENT = "system-admin";
    private static final String SYSTEM_ADMIN_CLIENT_SECRET = "systemAdminSecret";

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private LepManager lepManager;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilter(springSecurityFilterChain).build();
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.getSecurity().setDefaultClientSecret(DEFAULT_CLIENT_SECRET);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
    }

    @After
    public void destroy() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void testSystemAdminClientGetsTokenWithSystemAdminRole() {
        Map<String, Object> token = clientCredentialsToken(SYSTEM_ADMIN_CLIENT, SYSTEM_ADMIN_CLIENT_SECRET);

        assertThat(token).doesNotContainKey("refresh_token");
        String payload = new String(Base64.getUrlDecoder()
            .decode(token.get("access_token").toString().split("\\.")[1]));
        assertThat(new JacksonJsonParser().parseMap(payload).get("authorities")).asList()
            .containsExactly("SYSTEM-ADMIN");
    }

    @Test
    @SneakyThrows
    public void testSystemAdminClientWithWrongSecretIsRejected() {
        mockMvc.perform(post(TOKEN_URL)
                .param("grant_type", "client_credentials")
                .with(httpBasic(SYSTEM_ADMIN_CLIENT, "wrong")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @SneakyThrows
    public void testSystemAdminClientCanNotUsePasswordGrant() {
        mockMvc.perform(post(TOKEN_URL)
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .with(httpBasic(SYSTEM_ADMIN_CLIENT, SYSTEM_ADMIN_CLIENT_SECRET)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    public void testManagementRequiresSystemAdmin() {
        String systemAdmin = accessToken(SYSTEM_ADMIN_CLIENT, SYSTEM_ADMIN_CLIENT_SECRET);
        String superAdmin = accessToken(DEFAULT_CLIENT, DEFAULT_CLIENT_SECRET);

        mockMvc.perform(get("/management/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/env").header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/management/env").header(HttpHeaders.AUTHORIZATION, "Bearer " + systemAdmin))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    public void testAuditListIsDecidedByPrivilege() {
        String systemAdmin = accessToken(SYSTEM_ADMIN_CLIENT, SYSTEM_ADMIN_CLIENT_SECRET);
        String superAdmin = accessToken(DEFAULT_CLIENT, DEFAULT_CLIENT_SECRET);

        mockMvc.perform(get("/management/audits")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/management/audits").header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin))
            .andExpect(status().isOk());
        mockMvc.perform(get("/management/audits").header(HttpHeaders.AUTHORIZATION, "Bearer " + systemAdmin))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/management/audits/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdmin))
            .andExpect(status().isForbidden());
    }

    @Test
    @SneakyThrows
    public void testHealthIsPublicAndPrometheusIsClosed() {
        mockMvc.perform(get("/management/health").header(TENANT_HEADER, DEFAULT_TENANT_KEY_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        mockMvc.perform(get("/management/prometheus").header(TENANT_HEADER, DEFAULT_TENANT_KEY_VALUE))
            .andExpect(status().isUnauthorized());
    }

    @SneakyThrows
    private String accessToken(String client, String secret) {
        return clientCredentialsToken(client, secret).get("access_token").toString();
    }

    @SneakyThrows
    private Map<String, Object> clientCredentialsToken(String client, String secret) {
        String response = mockMvc.perform(post(TOKEN_URL)
                .param("grant_type", "client_credentials")
                .with(httpBasic(client, secret)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return new JacksonJsonParser().parseMap(response);
    }
}
