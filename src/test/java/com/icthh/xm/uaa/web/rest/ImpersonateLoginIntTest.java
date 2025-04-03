package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.uaa.security.oauth2.LepTokenGranterIntTest.loadFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.permission.service.PermissionService;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, XmOverrideConfiguration.class})
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = "TEST_ADMIN")
public class ImpersonateLoginIntTest {

    private static final String TENANT = "XM";
    private static final String INBOUND_TENANT = "DEMO";
    public static final String ROLE_TEST_OPERATOR = "ROLE_TEST_OPERATOR";
    public static final String ROLE_OPERATOR_READ_ONLY = "ROLE_OPERATOR_READ_ONLY";
    public static final String TEST_USER_ROLE = "ROLE_TEST_USER";
    public static final String TEST_ADMIN_ROLE = "ROLE_TEST_ADMIN";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String GRANT_TYPE = "grant_type";

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private TenantPropertiesService tenantPropertiesService;
    @MockBean
    private MailService mailService;
    @Autowired
    private MockMvc restMvc;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LepManager lepManager;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);

        TenantProperties properties = new TenantProperties();
        TenantProperties.Security security = new TenantProperties.Security();
        security.setDefaultUserRole(TEST_USER_ROLE);
        properties.setRegistrationCaptchaPeriodSeconds(0L);
        properties.setSecurity(security);
        objectMapper = new ObjectMapper(new YAMLFactory());
        tenantPropertiesService.onRefresh("/config/tenants/" + TENANT + "/uaa/uaa.yml",
            objectMapper.writeValueAsString(properties));
    }

    @Test
    @SneakyThrows
    public void testImpersonateLogin() {
        tenantPropertiesService.onRefresh("/config/tenants/XM/uaa/uaa.yml",
            loadFile("config/tenants/XM/impersonate-config.yml"));
        tenantPropertiesService.onRefresh("/config/tenants/DEMO/uaa/uaa.yml",
            loadFile("config/tenants/XM/impersonate-config.yml"));

        registerUser("admin@example.com", TEST_ADMIN_ROLE, "DEMO");
        registerUser("operator1@example.com", ROLE_TEST_OPERATOR, TENANT);
        registerUser("operator2@example.com", ROLE_TEST_OPERATOR, TENANT);
        registerUser("user@example.com", TEST_USER_ROLE, TENANT);
        registerUser("admin@example.com", TEST_ADMIN_ROLE, TENANT);

        impersonateLogin("admin@example.com", TENANT, "user@example.com", null, TEST_USER_ROLE, 360); // same tenant login
        impersonateLogin("admin@example.com", "DEMO", "user@example.com", TENANT, TEST_USER_ROLE, 360); // cross tenant login

        impersonateLogin("admin@example.com", TENANT, "operator1@example.com", null, ROLE_TEST_OPERATOR, 360);
        impersonateLogin("user@example.com", TENANT, "operator1@example.com", null, ROLE_OPERATOR_READ_ONLY, 36); // replace role
        callImpersonateLogin("admin@example.com", "DEMO", "operator1@example.com", TENANT).andExpect(
            status().isBadRequest()); // restrict by roles

        impersonateLogin("admin@example.com", TENANT, "operator2@example.com", null, ROLE_TEST_OPERATOR, 360);
        callImpersonateLogin("user@example.com", TENANT, "operator2@example.com", TENANT).andExpect(
            status().isBadRequest());  // restrict by logins

        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @SneakyThrows
    private void impersonateLogin(String inboundUser, String inboundTenant,
                                  String targetUser, String targetTenant, String targetRole, int tokenLifeTime){
        ResultActions actionResult = callImpersonateLogin(inboundUser, inboundTenant, targetUser, targetTenant);
        var result = actionResult.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        var token = objectMapper.readValue(result, OAuth2AccessToken.class);
        var tokenValue = new ObjectMapper().readValue(new String(getDecoder().decode(token.getValue().split("\\.")[1]), UTF_8), Map.class);
        assertEquals(targetRole, tokenValue.get("role_key"));
        assertEquals(targetTenant != null ? targetTenant : inboundTenant, tokenValue.get("tenant"));
        assertEquals(targetUser, tokenValue.get("user_name"));
        long exp = (int) tokenValue.get("exp");
        long ttl = exp * 1000 - (long)tokenValue.get("createTokenTime");
        assertTrue((tokenLifeTime - 100) * 1000L < ttl && ttl < (tokenLifeTime + 100) * 1000L);
    }

    @SneakyThrows
    private ResultActions callImpersonateLogin(String inboundUser, String inboundTenant, String targetUser, String targetTenant) {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(tenantContextHolder, inboundTenant);
        var accessToken = loginByUser(inboundUser, inboundTenant);
        String urlTemplate = "/oauth/impersonate/" + targetUser;
        if (targetTenant != null) {
            urlTemplate += "?tenant=" + targetTenant;
        }
        return restMvc.perform(
            post(urlTemplate)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .header("x-tenant", inboundTenant)
                .header("Authorization", "Bearer " + accessToken)
        );
    }


    private String loginByUser(String mail, String tenant) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(GRANT_TYPE, "password");
        params.add(USERNAME, mail);
        params.add(PASSWORD, "password");
        var result = restMvc.perform(
            post("/oauth/token")
                .params(params)
                .with(httpBasic("webapp", "webapp"))
                .header("x-tenant", tenant)
        ).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(result, Map.class).get("access_token").toString();
    }


    @SneakyThrows
    public void registerUser(String mail, String role, String tenant) {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(tenantContextHolder, tenant);

        String email = mail;

        UserLogin login = new UserLogin();
        login.setLogin(email);
        login.setTypeKey(UserLoginType.EMAIL.getValue());
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "password",             // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            true,                   //activated
            false, // tfaEnabled
            null, // tfaOtpChannelType
            null, // tfaOtpChannelSpec
            "http://placehold.it/50x50",  //imageUrl
            "en",                   // langKey
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            UUID.randomUUID().toString(),
            role,
            null, null, null, null,
            Collections.singletonList(login), false, null, null,
            List.of(role), null, true, null, null);

        roleService.onRefresh("/config/tenants/" + tenant + "/roles.yml",  loadFile("config/tenants/XM/impersonate-roles.yml"));
        permissionService.onRefresh("/config/tenants/" + tenant + "/permissions.yml", loadFile("config/tenants/XM/impersonate-permissions.yml"));

        restMvc.perform(
                post("/api/register")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .header("x-tenant", tenant)
                    .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(tenantContextHolder, tenant);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        Optional<User> user = userService.findOneByLogin(email);
        user.get().setActivated(true);
        user.get().setAuthorities(List.of(role));
        user.get().setRoleKey(role);
        userRepository.save(user.get());

        lepManager.endThreadContext();

    }

}
