package com.icthh.xm.uaa.security.oauth2.tfa;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.LepTextConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    LepTextConfiguration.class,
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class TfaOtpAuthenticationIntTest {

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenStore tokenStore;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    @MockBean
    private OtpGenerator otpGenerator;

    private MockMvc mockMvc;

    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                                      .addFilter(springSecurityFilterChain).build();

        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });

        TenantProperties tenantProperties = new TenantProperties();

        tenantProperties.getSecurity().setDefaultClientSecret("cleintSecret");
        tenantProperties.getSecurity().setTfaEnabled(true);
        tenantProperties.getSecurity().setTfaDefaultOtpChannelType("email");
        tenantProperties.getSecurity().setTfaEnabledOtpChannelTypes(Set.of(OtpChannelType.EMAIL));

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        when(otpGenerator.generate(any())).thenReturn("123456");

    }

    @After
    public void destroy() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    public void testUserConsumesApiNoTFA() throws Exception {

        // 1. create User (no TFA)
        createUser("notfauser", "password1", false);

        // 2. obtain access token by user/pass
        OAuth2AccessToken token = obtainAccessToken("notfauser", "password1");

        // 3. Use API with accessToken
        consumeAPISuccessfully(token, "notfauser");

    }

    @Test
    public void testUserConsumesApiWithTFA() throws Exception {

        // 1. Create user with TFA enabled
        createUser("testuser", "password2", true);

        // 2. obtain access token by user/pass but in response is tfatoken
        OAuth2AccessToken tfaToken = obtainAccessToken("testuser", "password2");

        Map<String, Object> tokenDetails = tfaToken.getAdditionalInformation();
        assertNotNull(tokenDetails);
        assertNotNull(tokenDetails.get("tfaVerificationKey"));
        assertEquals("email", tokenDetails.get("tfaOtpChannel"));
        assertEquals("testuser", tokenDetails.get("user_name"));

        // 3. Try to use API with tfaToken, it should fail
        mockMvc.perform(get("/api/account")
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tfaToken.getValue()))
               .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
               .andExpect(status().isUnauthorized())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$.error").value("invalid_token"))
               .andExpect(jsonPath("$.error_description").value("TfaAccess token can not be used to access API"));

        // 4. Obtain access token by second factor (OTP)
        OAuth2AccessToken accessToken = obtainAccessTokenByOtp(tfaToken.getValue());

        assertFalse(accessToken.getAdditionalInformation().containsKey("tfaVerificationKey"));
        assertFalse(accessToken.getAdditionalInformation().containsKey("tfaOtpChannel"));

        // 5. Try to use API with access token, it should be success
        consumeAPISuccessfully(accessToken, "testuser");
    }

    private void createUser(String login, String password, boolean tfaEnabled) {
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
        user.setTfaEnabled(tfaEnabled);
        user.setUpdatePasswordDate(Instant.now().plus(1, ChronoUnit.DAYS));

        userRepository.saveAndFlush(user);

    }

    private OAuth2AccessToken obtainAccessToken(String username, String password) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("username", username);
        params.add("password", password);

        return getOAuth2AccessToken(params);

    }

    private OAuth2AccessToken obtainAccessTokenByOtp(String tfaToken) {

        System.out.println(tfaToken);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "tfa_otp_token");
        params.add("tfa_access_token_type", "bearer");
        params.add("otp", "123456");
        params.add("tfa_access_token", tfaToken);

        return getOAuth2AccessToken(params);

    }

    private void consumeAPISuccessfully(OAuth2AccessToken token, String userlogin) throws Exception {
        mockMvc.perform(get("/api/account")
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token.getValue()))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$.firstName").value(userlogin))
               .andExpect(jsonPath("$.logins[0].login").value(userlogin))
               .andExpect(jsonPath("$.logins[0].typeKey").value("LOGIN.EMAIL"))
               .andExpect(jsonPath("$.roleKey").value("SUPER-ADMIN"));
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private OAuth2AccessToken getOAuth2AccessToken(final MultiValueMap<String, String> params) {
        ResultActions result
            = mockMvc.perform(post("/oauth/token")
                                  .params(params)
                                  .with(httpBasic("testClient", "cleintSecret"))
                                  .accept("application/json;charset=UTF-8"))
                     .andExpect(status().isOk())
                     .andExpect(content().contentType("application/json;charset=UTF-8"));

        String resultString = result.andReturn().getResponse().getContentAsString();

        JacksonJsonParser jsonParser = new JacksonJsonParser();
        String token = jsonParser.parseMap(resultString).get("access_token").toString();
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(token);
        Map<String, Object> additionalDetails = (Map<String, Object>) accessToken.getAdditionalInformation()
                                                                                 .get("additionalDetails");

        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        if (additionalDetails != null) {
            assertFalse(additionalDetails.containsKey("otp"));
            assertFalse(additionalDetails.containsKey("tfa_access_token"));
            assertFalse(additionalDetails.containsKey("tfa_access_token_type"));
        }

        return accessToken;
    }

}
