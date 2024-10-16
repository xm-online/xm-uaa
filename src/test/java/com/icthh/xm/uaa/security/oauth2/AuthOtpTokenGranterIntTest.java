package com.icthh.xm.uaa.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.LepConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.util.RandomUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class,
    LepConfiguration.class
})
public class AuthOtpTokenGranterIntTest {

    private static final String TENANT = "XM";

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String CLIENT_ROLE_KEY = "ROLE_KEY";

    private static final String OTP_CODE = "123456";
    private static final String USERNAME = "user@test.com.ua";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Value("${:classpath:config/tenants/XM/uaa/uaa.yml}")
    private Resource spec;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private AuthorizationServerTokenServices tokenServices;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainUserDetailsService domainUserDetailsService;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    private AuthOtpTokenGranter authOtpTokenGranter;

    @Before
    public void setUp() throws IOException {
        authOtpTokenGranter = new AuthOtpTokenGranter(domainUserDetailsService, tokenServices, clientDetailsService,
            new DefaultOAuth2RequestFactory(clientDetailsService));

        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });

        String conf = StreamUtils.copyToString(spec.getInputStream(), Charset.defaultCharset());
        TenantProperties tenantProperties = objectMapper.readValue(conf, TenantProperties.class);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        // create client
        clientRepository.save(buildClient());
    }

    @After
    public void destroy() {
        clientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testValidOtpCode_shouldReturnAccessToken() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        OAuth2AccessToken result = authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest);

        assertNotNull(result);
        assertNotNull(result.getValue());
        assertNotNull(result.getRefreshToken());
        assertNotNull(result.getExpiration());
        assertEquals(result.getTokenType(), "bearer");
        assertEquals(result.getAdditionalInformation().get("user_key"), user.getUserKey());
        assertEquals(result.getAdditionalInformation().get("tenant"), TENANT);
    }

    @Test
    public void testMissingUsername_shouldThrowInvalidGrantException() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("Missing token request param: username", InvalidGrantException.class, () ->
            authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testMissingOtp_shouldThrowInvalidGrantException() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("Missing token request param: auth_otp", InvalidGrantException.class, () ->
            authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testInvalidUsername_shouldThrowUsernameNotFoundException() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);
        requestParameters.put("username", "invalidUserName");

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("User [invalidUserName] was not found for tenant [XM]",
            UsernameNotFoundException.class,
            () -> authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testInvalidOtp() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", "invalidOtpCode");
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("Authorization otp code is invalid", InvalidGrantException.class,
            () -> authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testUserNotActive_shouldThrowInvalidGrantException() {
        User user = buildUser();
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("User user@test.com.ua was not activated", InvalidGrantException.class,
            () -> authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testInvalidClientId() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, "invalidClientId", Collections.emptyList(), GrantType.OTP.getValue());

        assertThrows("Client was not found: invalidClientId", ClientRegistrationException.class,
            () -> authOtpTokenGranter.grant(GrantType.OTP.getValue(), tokenRequest)
        );
    }

    @Test
    public void testDifferentGrantType() {
        User user = buildUser();
        user.setActivated(true);
        user.setOtpCode(OTP_CODE);
        userRepository.save(user);

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("auth_otp", OTP_CODE);
        requestParameters.put("username", USERNAME);

        TokenRequest tokenRequest = new TokenRequest(requestParameters, CLIENT_ID, Collections.emptyList(), GrantType.PASSWORD.getValue());

        assertNull(authOtpTokenGranter.grant(GrantType.PASSWORD.getValue(), tokenRequest));
    }

    public User buildUser() {
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.getByRegex(USERNAME).getValue());
        userLogin.setLogin(USERNAME);

        User user = new User();
        user.setUserKey(UUID.randomUUID().toString());
        user.setPassword(RandomStringUtils.random(60));
        user.setPasswordSetByUser(false);
        user.setLangKey("ua");
        user.setActivationKey(RandomUtil.generateActivationKey());
        user.setRoleKey("ROLE_VIEW");
        user.setLogins(List.of(userLogin));
        user.getLogins().forEach(ul -> ul.setUser(user));
        return user;
    }

    private Client buildClient() {
        return new Client()
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)
            .roleKey(CLIENT_ROLE_KEY)
            .description("DEFAULT_DESCRIPTION");
    }
}
