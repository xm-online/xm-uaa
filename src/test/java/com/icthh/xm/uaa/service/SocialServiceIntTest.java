package com.icthh.xm.uaa.service;

import static com.google.common.collect.ImmutableBiMap.of;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.REGISTERED;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.SING_IN;
import static com.icthh.xm.uaa.utils.DeepReflectionEquals.deepRefEq;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.Tenant;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.SocialUserConnection;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.domain.properties.TenantProperties.UserInfoMapping;
import com.icthh.xm.uaa.repository.SocialUserConnectionRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.DomainTokenServices;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.social.ConfigOAuth2Api;
import com.icthh.xm.uaa.social.ConfigOAuth2Template;
import com.icthh.xm.uaa.social.ConfigServiceProvider;
import com.icthh.xm.uaa.social.SocialLoginAnswer;
import com.icthh.xm.uaa.social.SocialUserInfoMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.support.RestGatewaySupport;

@Slf4j
public class SocialServiceIntTest {

    private static final String MOCK_ACCESS_TOKEN = "MOCK_ACCESS_TOKEN";
    public static final String PROVIDER_ID = "P_ID";
    public static final String PROVIDER_USER_ID = "123";

    private SocialService socialService;

    @Mock
    private  SocialUserConnectionRepository socialRepository;
    @Mock
    private  PasswordEncoder passwordEncoder;
    @Mock
    private  UserRepository userRepository;
    @Mock
    private  AccountMailService accountMailService;

    private  UserDetailsService userDetailsService;
    @Mock
    private  TenantPropertiesService tenantPropertiesService;

    private  DomainTokenServices tokenServices;
    @Mock
    private  UserLoginRepository userLoginRepository;

    private  SocialUserInfoMapper socialUserInfoMapper = new SocialUserInfoMapper();
    @Mock
    private  XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Mock
    private  XmRequestContextHolder xmRequestContextHolder;
    @Mock
    private TenantContextHolder tenantContextHolder;

    private MockRestServiceServer oAuth2TemplateMockServer;
    List<Consumer<?>> oAuth2RestMoks = new ArrayList<>();

    private MockRestServiceServer apiMockSserver;
    List<Consumer<?>> apiRestMoks = new ArrayList<>();

    private static final String ROLE_USER = "ROLE_USER";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        userDetailsService = new DomainUserDetailsService(userLoginRepository, tenantContextHolder);
        tokenServices = new DomainTokenServices();
        DomainJwtAccessTokenConverter accessTokenEnhancer = new DomainJwtAccessTokenConverter(tenantContextHolder);
        tokenServices.setTokenStore(new JwtTokenStore(accessTokenEnhancer));
        tokenServices.setTokenConstraintsService(mock(TokenConstraintsService.class));
        tokenServices.setTokenEnhancer(accessTokenEnhancer);

        socialService = new SocialService(socialRepository,
                                          passwordEncoder,
                                          userRepository,
                                          accountMailService,
                                          userDetailsService,
                                          tenantPropertiesService,
                                          tokenServices,
                                          userLoginRepository,
                                          socialUserInfoMapper,
                                          xmAuthenticationContextHolder,
                                          xmRequestContextHolder,
                                          null) {
            @Override
            protected ConfigServiceProvider createConfigServiceProvider(Social social) {
                return new ConfigServiceProvider(social, socialUserInfoMapper) {
                    @Override
                    protected ConfigOAuth2Template createOAuth2Template(Social social) {
                        ConfigOAuth2Template oAuth2Template = super.createOAuth2Template(social);

                        RestGatewaySupport gateway = new RestGatewaySupport();
                        gateway.setRestTemplate(oAuth2Template.getRestTemplate());
                        oAuth2TemplateMockServer = MockRestServiceServer.createServer(gateway);
                        oAuth2RestMoks.forEach(it -> it.accept(null));

                        return oAuth2Template;
                    }

                    @Override
                    public ConfigOAuth2Api getApi(String accessToken) {
                        ConfigOAuth2Api api = super.getApi(accessToken);

                        RestGatewaySupport gateway = new RestGatewaySupport();
                        gateway.setRestTemplate(api.getRestTemplate());
                        apiMockSserver = MockRestServiceServer.createServer(gateway);
                        apiRestMoks.forEach(it -> it.accept(null));

                        return api;
                    }
                };
            }
        };
        socialService.setSelf(socialService);
    }

    @Test(expected = ProviderNotFoundException.class)
    public void testProviderNotFound() {
        mockTenantProperties();
        socialService.initSocialLogin("unknow provider");
    }

    @Test
    public void testInitUrl() {
        mockTenantProperties();
        mockRequestContext();
        String result = socialService.initSocialLogin("P_ID");
        Assert.assertEquals(result, "http://AU?client_id=CI&response_type=code&redirect_uri=http%3A%2F%2Fdomainname%3A0987%2Fuaa%2Fsocial%2Fsignin%2FP_ID&scope=SCOPE");
    }

    @Test
    @SneakyThrows
    public void testSuccessSingIn() {
        mockTenantProperties();
        mockRequestContext();
        mockGetAccessRequests();

        SocialUserConnection userConnection = new SocialUserConnection();
        userConnection.setUserKey("USER_KEY");
        when(socialRepository.findByProviderUserIdAndProviderId(PROVIDER_USER_ID, PROVIDER_ID)).thenReturn(
            Optional.of(userConnection));

        User user = new User();
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setUser(user);
        userLogin.setLogin("test@email.com");

        UserLogin number = new UserLogin();
        number.setTypeKey(UserLoginType.MSISDN.getValue());
        number.setUser(user);
        number.setLogin("380930912700");
        user.setActivated(true);
        user.setRoleKey("ROLE_USER");
        user.setUserKey("USER_KEY");
        user.setPassword("password");

        user.setLogins(asList(userLogin, number));
        when(userRepository.findOneByUserKey("USER_KEY")).thenReturn(Optional.of(user));
        when(userLoginRepository.findOneByLogin("test@email.com")).thenReturn(Optional.of(userLogin));

        mockTenant();

        SocialLoginAnswer socialLoginAnswer = socialService.acceptSocialLoginUser("P_ID", "activationCode");

        Assert.assertEquals(socialLoginAnswer.getAnswerType(), SING_IN);

        assertJwtToken(socialLoginAnswer, "test@email.com",
                       asList(login("LOGIN.EMAIL", "test@email.com"), login("LOGIN.MSISDN", "380930912700")));
    }

    private void assertJwtToken(SocialLoginAnswer socialLoginAnswer, String userName,
                                List<Map<String, Object>> logins) throws java.io.IOException {
        Map<String, Object> jwt = new HashMap<>();
        jwt.put("user_name", userName);
        jwt.put("scope", asList("openid"));
        jwt.put("role_key", "ROLE_USER");
        jwt.put("user_key", "USER_KEY");
        jwt.put("additionalDetails", emptyMap());
        jwt.put("logins", logins);
        jwt.put("authorities", asList("ROLE_USER"));
        jwt.put("tenant", "TEST_T");
        jwt.put("client_id", "webapp");

        log.info("{}", socialLoginAnswer.getOAuth2AccessToken().getValue());
        String value = socialLoginAnswer.getOAuth2AccessToken().getValue().split("\\.")[1];
        Map map = new ObjectMapper().readValue(Base64.getDecoder().decode(value), Map.class);
        map.remove("createTokenTime");
        map.remove("jti");

        Assert.assertEquals(jwt, map);
    }

    @Test
    @SneakyThrows
    public void createUserAutomaticallyByDefault() {
        mockTenantProperties();
        mockRequestContext();
        mockGetAccessRequests();

        when(socialRepository.findByProviderUserIdAndProviderId(PROVIDER_USER_ID, PROVIDER_ID)).thenReturn(empty());

        User user = createMockUser();

        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setUser(user);
        userLogin.setLogin("test@email.com");
        UserLogin number = new UserLogin();
        number.setTypeKey(UserLoginType.MSISDN.getValue());
        number.setUser(user);
        number.setLogin("380930912700");
        user.setLogins(asList(userLogin, number));
        when(userRepository.findOneByUserKey("USER_KEY")).thenReturn(Optional.of(user));
        when(userLoginRepository.findOneByLoginIgnoreCase("test@email.com")).thenReturn(empty());
        when(userLoginRepository.findOneByLoginIgnoreCase("380930912700")).thenReturn(empty());
        when(userLoginRepository.findOneByLogin("test@email.com")).thenReturn(Optional.of(userLogin));
        mockTenant();
        when(userRepository.save(any(User.class))).thenReturn(user);
        SocialUserConnection userConnection = new SocialUserConnection(null, "USER_KEY", PROVIDER_ID, PROVIDER_USER_ID,
                                                                       null, null);
        when(socialRepository.save(any(SocialUserConnection.class))).thenReturn(userConnection);

        SocialLoginAnswer socialLoginAnswer = socialService.acceptSocialLoginUser("P_ID", "activationCode");

        user.setPassword(null);
        verify(userRepository).save(deepRefEq(user, "userKey", "tfaOtpSecret", "createdDate", "lastModifiedDate"));
        verify(socialRepository).save(refEq(userConnection, "activationCode"));
        Assert.assertEquals(socialLoginAnswer.getAnswerType(), REGISTERED);
        assertJwtToken(socialLoginAnswer, "test@email.com",
                       asList(login("LOGIN.EMAIL", "test@email.com"), login("LOGIN.MSISDN", "380930912700")));
    }

    @Test
    @SneakyThrows
    public void createUserAutomaticallyWithOnlyPhoneNumber() {
        mockTenantProperties();
        mockRequestContext();
        mockRequestToken();
        apiRestMoks.add(r -> {
            Map<String, Object> user = new HashMap<>();
            user.put("fn", "firstN");
            user.put("id", 123);
            user.put("path", of("to", of("to", of("image", of("url", "URL_IMAGE")))));
            user.put("phoneNumher", "380930912700");
            apiMockSserver.expect(times(2), requestTo("http://UIU"))
                          .andExpect(method(HttpMethod.GET))
                          .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + MOCK_ACCESS_TOKEN))
                          .andRespond(withSuccess(toJson(user), MediaType.APPLICATION_JSON));

        });

        when(socialRepository.findByProviderUserIdAndProviderId(PROVIDER_USER_ID, PROVIDER_ID)).thenReturn(empty());

        User user = createMockUser();

        UserLogin number = new UserLogin();
        number.setTypeKey(UserLoginType.MSISDN.getValue());
        number.setUser(user);
        number.setLogin("380930912700");
        user.setLogins(asList(number));

        when(userRepository.findOneByUserKey("USER_KEY")).thenReturn(Optional.of(user));
        when(userLoginRepository.findOneByLoginIgnoreCase("380930912700")).thenReturn(empty());
        when(userLoginRepository.findOneByLogin("380930912700")).thenReturn(Optional.of(number));
        mockTenant();
        when(userRepository.save(any(User.class))).thenReturn(user);
        SocialUserConnection userConnection = new SocialUserConnection(null, "USER_KEY", PROVIDER_ID, PROVIDER_USER_ID,
                                                                       null, null);
        when(socialRepository.save(any(SocialUserConnection.class))).thenReturn(userConnection);

        SocialLoginAnswer socialLoginAnswer = socialService.acceptSocialLoginUser("P_ID", "activationCode");

        user.setPassword(null);
        verify(userRepository).save(deepRefEq(user, "userKey", "tfaOtpSecret", "createdDate", "lastModifiedDate"));
        verify(socialRepository).save(refEq(userConnection, "activationCode"));
        Assert.assertEquals(socialLoginAnswer.getAnswerType(), REGISTERED);
        assertJwtToken(socialLoginAnswer, "380930912700", asList(login("LOGIN.MSISDN", "380930912700")));
    }

    private void mockRequestToken() {
        oAuth2RestMoks.add(r -> {
            oAuth2TemplateMockServer.expect(once(), requestTo("http://ATU"))
                                    .andExpect(method(HttpMethod.POST))
                                    .andExpect(content().string("client_id=CI&client_secret=CS&code=activationCode&redirect_uri=http%3A%2F%2Fdomainname%3A0987%2Fuaa%2Fsocial%2Fsignin%2FP_ID&grant_type=authorization_code"))
                                    .andRespond(withSuccess("{\"access_token\": \"" + MOCK_ACCESS_TOKEN + "\"}", MediaType.APPLICATION_JSON));
        });
    }

    private User createMockUser() {
        User user = new User();
        user.setActivated(true);
        user.setRoleKey("ROLE_USER");
        user.setUserKey("USER_KEY");
        user.setPassword("PWD");
        user.setFirstName("firstN");
        user.setLangKey(null);
        return user;
    }

    private Map<String, Object> login(String typeKey, String value) {
        Map<String, Object> login = new HashMap<>();
        login.put("typeKey", typeKey);
        login.put("stateKey", null);
        login.put("login", value);
        return login;
    }

    private void mockTenant() {
        when(tenantContextHolder.getContext()).thenReturn(new TenantContext() {

            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public Optional<Tenant> getTenant() {
                return empty();
            }

            @Override
            public Optional<TenantKey> getTenantKey() {
                return Optional.of(TenantKey.valueOf("TEST_T"));
            }
        });
    }

    private void mockGetAccessRequests() {
        mockRequestToken();
        apiRestMoks.add(r -> {
            Map<String, Object> user = new HashMap<>();
            user.put("email", "test@email.com");
            user.put("fn", "firstN");
            user.put("id", 123);
            user.put("path", of("to", of("to", of("image", of("url", "URL_IMAGE")))));
            user.put("phoneNumher", "380930912700");
                apiMockSserver.expect(times(2), requestTo("http://UIU"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + MOCK_ACCESS_TOKEN))
                    .andRespond(withSuccess(toJson(user), MediaType.APPLICATION_JSON));

        });
    }

    @SneakyThrows
    private String toJson(Map<String, Object> user) {
        return new ObjectMapper().writeValueAsString(user);
    }

    private void mockRequestContext() {
        XmRequestContext mock = mock(XmRequestContext.class);
        when(xmRequestContextHolder.getContext()).thenReturn(mock);
        when(mock.getValue("protocol", String.class)).thenReturn("http");
        when(mock.getValue("domain", String.class)).thenReturn("domainname");
        when(mock.getValue("port", String.class)).thenReturn("0987");
    }

    private void mockTenantProperties() {
        TenantProperties tenantProperties = new TenantProperties();
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        Social social = new Social();
        tenantProperties.setSocial(asList(social));
        social.setAccessTokenUrl("http://ATU");
        social.setAuthorizeUrl("http://AU");
        social.setClientId("CI");
        social.setClientSecret("CS");
        social.setProviderId("P_ID");
        social.setScope("SCOPE");
        social.setUserInfoUri("http://UIU");
        UserInfoMapping userInfoMapping = new UserInfoMapping();
        social.setUserInfoMapping(userInfoMapping);
        userInfoMapping.setEmail("email");
        userInfoMapping.setFirstName("fn");
        userInfoMapping.setLastName("ln");
        userInfoMapping.setId("id");
        userInfoMapping.setLangKey("path.to.lang");
        userInfoMapping.setImageUrl("path.to.image.url");
        userInfoMapping.setPhoneNumber("phoneNumher");
        TenantProperties.Security security = new TenantProperties.Security();
        security.setDefaultUserRole("ROLE_USER");
        tenantProperties.setSecurity(security);
    }

    private User createExistingUser(String email,
                                    String firstName,
                                    String lastName,
                                    String imageUrl) {
        User user = new User();
        user.setUserKey("test");
        user.setRoleKey(ROLE_USER);
        user.setPassword("password");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setImageUrl(imageUrl);
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin(email);
        userLogin.setUser(user);
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        user.getLogins().add(userLogin);
        return user;
    }
}

