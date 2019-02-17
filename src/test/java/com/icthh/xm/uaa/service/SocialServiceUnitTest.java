package com.icthh.xm.uaa.service;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.domain.properties.TenantProperties.UserInfoMapping;
import com.icthh.xm.uaa.repository.SocialUserConnectionRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.social.SocialLoginAnswer;
import com.icthh.xm.uaa.social.SocialUserInfoMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.support.RestGatewaySupport;

public class SocialServiceUnitTest {

    private SocialService socialService;

    @Mock
    private  SocialUserConnectionRepository socialRepository;
    @Mock
    private  PasswordEncoder passwordEncoder;
    @Mock
    private  UserRepository userRepository;
    @Mock
    private  AccountMailService accountMailService;
    @Mock
    private  UserDetailsService userDetailsService;
    @Mock
    private  TenantPropertiesService tenantPropertiesService;
    @Mock
    private  AuthorizationServerTokenServices tokenServices;
    @Mock
    private  UserLoginRepository userLoginRepository;
    @Mock
    private  SocialUserInfoMapper socialUserInfoMapper;
    @Mock
    private  XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Mock
    private  XmRequestContextHolder xmRequestContextHolder;

    private MockRestServiceServer mockServer;

    private static final String ROLE_USER = "ROLE_USER";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
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
                                          null);
        socialService.setSelf(socialService);

        RestGatewaySupport gateway = new RestGatewaySupport();
        gateway.setRestTemplate(restTemplate);
        mockServer = MockRestServiceServer.createServer(gateway);
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
    public void test() {
        mockTenantProperties();
        mockRequestContext();
        SocialLoginAnswer socialLoginAnswer = socialService.acceptSocialLoginUser("P_ID", "activationCode");
    }

    private void mockRequest() {
        mockServer.expect(once(), requestTo("http://ATU"))
            .andExpect(method(HttpMethod.GET))
            //.andExpect(header())
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
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
        userInfoMapping.setId("id");
        userInfoMapping.setLangKey("path.to.lang");
        userInfoMapping.setImageUrl("path.to.image.url");
        userInfoMapping.setPhoneNumber("phoneNumher");
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

