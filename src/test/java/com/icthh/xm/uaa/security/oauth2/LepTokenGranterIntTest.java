package com.icthh.xm.uaa.security.oauth2;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.LepConfiguration;
import com.icthh.xm.uaa.config.xm.LepTextConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    LepTextConfiguration.class,
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class LepTokenGranterIntTest {

    public static final String LEP_PATH_TEST_GRAND_TYPE = "/config/tenants/XM/uaa/lep/security/oauth2/"
        + "TokenGranter$$test_grant_type$$around.groovy";

    public static final String LEP_PATH_TEST_GRAND_TYPE_NOT_IN_APP_PROPS = "/config/tenants/XM/uaa/lep/security/oauth2/"
        + "TokenGranter$$test_grant_type_not_in_app_properties$$around.groovy";

    public static final String USER_NAME = "user";
    public static final String USER_PASS = "password1";
    public static final String TOKEN_URL = "/oauth/token";
    public static final String CLIENT_NAME = "testClient";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String GRANT_TYPE = "grant_type";
    public static final String TEST_GRANT_TYPE = "test_grant_type";
    public static final String TEST_WRONG_GRANT_TYPE = "test_wrong_grant_type";
    public static final String TEST_GRANT_TYPE_NOT_IN_APP_PROPERTIES = "test_grant_type_not_in_app_properties";

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenStore tokenStore;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).addFilter(springSecurityFilterChain).build();
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });

        leps.onRefresh(LEP_PATH_TEST_GRAND_TYPE, loadFile(LEP_PATH_TEST_GRAND_TYPE));
        leps.onRefresh(LEP_PATH_TEST_GRAND_TYPE_NOT_IN_APP_PROPS, loadFile(LEP_PATH_TEST_GRAND_TYPE_NOT_IN_APP_PROPS));
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.getSecurity().setDefaultClientSecret(CLIENT_SECRET);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
    }

    @After
    public void destroy() {
        leps.onRefresh(LEP_PATH_TEST_GRAND_TYPE, null);
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    @SneakyThrows
    public void testCustomGrantType() {
        createUser(USER_NAME, USER_PASS);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(GRANT_TYPE, TEST_GRANT_TYPE);
        params.add(USERNAME, USER_NAME);
        params.add(PASSWORD, USER_PASS);

        ResultActions result = mockMvc.perform(post(TOKEN_URL)
                .params(params)
                .with(httpBasic(CLIENT_NAME, CLIENT_SECRET))
                .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));

        String resultString = result.andReturn().getResponse().getContentAsString();

        JacksonJsonParser jsonParser = new JacksonJsonParser();
        String token = jsonParser.parseMap(resultString).get("access_token").toString();
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);

        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());
        userRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    public void testWrongCustomGrantType() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(GRANT_TYPE, TEST_WRONG_GRANT_TYPE);
        params.add(USERNAME, USER_NAME);
        params.add(PASSWORD, USER_PASS);

        checkUnsupportedGrantTypeException(params);
    }

    @Test
    @SneakyThrows
    public void testCustomGrantTypeNotInApplicationProperties() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(GRANT_TYPE, TEST_GRANT_TYPE_NOT_IN_APP_PROPERTIES);
        params.add(USERNAME, USER_NAME);
        params.add(PASSWORD, USER_PASS);

        checkUnsupportedGrantTypeException(params);
    }

    private void checkUnsupportedGrantTypeException(MultiValueMap<String, String> params) throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                .params(params)
                .with(httpBasic(CLIENT_NAME, CLIENT_SECRET))
                .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(ex -> assertTrue(ex.getResolvedException() instanceof UnsupportedGrantTypeException));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), UTF_8);
    }

    private void createUser(String login, String password) {
        User user = new User();

        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(login);
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setUser(user);

        user.setFirstName(login);
        user.setLogins(List.of(userLogin));
        user.setRoleKey(RoleConstant.SUPER_ADMIN);
        user.setUserKey(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordSetByUser(true);
        user.setActivated(true);
        user.setTfaEnabled(true);
        user.setUpdatePasswordDate(Instant.now().plus(1, ChronoUnit.DAYS));

        userRepository.saveAndFlush(user);

    }

}
