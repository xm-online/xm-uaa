package com.icthh.xm.uaa.web.rest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.SocialBeanOverrideConfiguration;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.SocialConfig;
import com.icthh.xm.uaa.domain.SocialUserConnection;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.SocialConfigRepository;
import com.icthh.xm.uaa.service.MailService;
import com.icthh.xm.uaa.service.SocialService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.social.connect.web.ConnectSupport;
import com.icthh.xm.uaa.social.connect.web.ProviderSignInAttempt;
import com.icthh.xm.uaa.social.connect.web.ProviderSignInUtils;
import com.icthh.xm.uaa.social.connect.web.SessionStrategy;
import com.icthh.xm.uaa.social.twitter.api.TwitterProfile;
import lombok.SneakyThrows;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Test class for the SocialController REST controller.
 *
 * @see SocialController
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, SocialBeanOverrideConfiguration.class, XmOverrideConfiguration.class})
public class SocialControllerIntTest {

    private static final String DEFAULT_SCHEME = "my";
    private static final String DEFAULT_DOMAIN = "xm.localhost";
    private static final String DEFAULT_PORT = "777";
    private static final String DEFAULT_TENANT = "XM";
    private static final String DEFAULT_LOGIN = "admin";

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private SocialService socialService;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private MailService mailService;

    @Autowired
    private ProviderSignInUtils providerSignInUtils;

    @Autowired
    private ConnectionFactoryLocator connectionFactoryLocator;

    @Autowired
    private UsersConnectionRepository usersConnectionRepository;

    @Autowired
    private SignInAdapter signInAdapter;

    @Autowired
    private ConnectSupport connectSupport;

    @Autowired
    private SessionStrategy sessionStrategy;

    @Mock
    private Connection<?> connection;

    @Mock
    private ProviderSignInAttempt providerSignInAttempt;

    @Autowired
    private SocialConfigRepository socialConfigRepository;

    private MockMvc restUserMockMvc;

    @BeforeClass
    public static void init() {
        TenantContext.setCurrent(new TenantInfo( DEFAULT_TENANT, DEFAULT_LOGIN, "", DEFAULT_SCHEME, DEFAULT_DOMAIN, DEFAULT_PORT,""));
    }

    @AfterClass
    public static void tearDown() {
        TenantContext.clear();
    }

    @Before
    @SneakyThrows
    public void setup() {
        TenantProperties properties = new TenantProperties();
        properties.setSocial(asList(
            new TenantProperties.Social("twitter", "xxx", "yyy", DEFAULT_DOMAIN),
            new TenantProperties.Social("facebook", "xxx", "yyy", DEFAULT_DOMAIN)
        ));
        tenantPropertiesService.onRefresh("/config/tenants/"+ DEFAULT_TENANT + "/uaa/uaa.yml",
            new ObjectMapper(new YAMLFactory()).writeValueAsString(properties));

        MockitoAnnotations.initMocks(this);
        SocialController socialController = new SocialController(socialService, providerSignInUtils,
            connectionFactoryLocator, usersConnectionRepository, signInAdapter, connectSupport, sessionStrategy, socialConfigRepository);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(socialController)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
        when(sessionStrategy.getAttribute(any(RequestAttributes.class), eq(ProviderSignInAttempt.SESSION_ATTRIBUTE)))
            .thenReturn(providerSignInAttempt);
        when(connection.fetchUserProfile())
            .thenReturn(new UserProfile("id", "name", "fname", "lname", "email", "username"));
        when(connection.createData()).thenReturn(
            new ConnectionData("twitter", "providerUserId", "displayName", "profileUrl", "imageUrl", "", "secret",
                "refreshToken", 1000L));
        Mockito.<Connection<?>>when(connectSupport.completeConnection(any(OAuth1ConnectionFactory.class), any()))
            .thenReturn(connection);
        Mockito.<Connection<?>>when(connectSupport.completeConnection(any(OAuth2ConnectionFactory.class), any()))
            .thenReturn(connection);
        when(connectSupport.buildOAuthUrl(any(), any(), any())).thenReturn("SomeCallbackUrl");
        Mockito.<Connection<?>>when(providerSignInAttempt.getConnection(any())).thenReturn(connection);
    }

    @Test
    @Transactional
    public void testSignupRedirectProvider() throws Exception {
        when(connection.getKey()).thenReturn(new ConnectionKey("testProvider", "providerUserId"));
        restUserMockMvc.perform(get("/social/signup"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location",
                "my://xm.localhost:777/social-register/testProvider?success=true"));
    }

    @Test
    @Transactional
    public void testSigninNoProvider() throws Exception {
        restUserMockMvc.perform(post("/social/signin/badprovider"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location",
                containsString("my://xm.localhost:777/social-register/badprovider?success=false")));
    }

    @Test
    @Transactional
    public void testSigninTwitter() throws Exception {
        restUserMockMvc.perform(post("/social/signin/twitter"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("SomeCallbackUrl")));
    }

    @Test
    @Transactional
    public void testOauth1Callback() throws Exception {
        when(connection.getKey()).thenReturn(new ConnectionKey("twitter", "providerUserId"));
        restUserMockMvc.perform(get("/social/signin/twitter")
            .param("oauth_token", "token"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "my://xm.localhost:777/uaa/social/signup"));
    }

    @Test
    @Transactional
    public void testOauth2Callback() throws Exception {
        when(connection.getKey()).thenReturn(new ConnectionKey("facebook", "providerUserId"));
        restUserMockMvc.perform(get("/social/signin/facebook")
            .param("code", "code"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "my://xm.localhost:777/uaa/social/signup"));

    }

    @Test
    @Transactional
    public void equalsVerifierSocialUserConnection() throws Exception {
        SocialUserConnection userA = new SocialUserConnection();
        userA.setId(100L);
        SocialUserConnection userAA = new SocialUserConnection();
        userAA.setId(100L);
        SocialUserConnection userB = new SocialUserConnection();
        userB.setId(200L);
        assertThat(userA).isNotEqualTo(userB);
        assertThat(userA).isEqualTo(userAA);
        assertThat(userA.hashCode()).isEqualTo(userAA.hashCode());
        assertThat(userA.toString()).isNotBlank();
    }

    @Test
    @Transactional
    public void equalsVerifierSocialConfig() throws Exception {
        SocialConfig userA = new SocialConfig();
        userA.setProviderId("facebook");
        SocialConfig userAA = new SocialConfig();
        userAA.setProviderId("facebook");
        SocialConfig userB = new SocialConfig();
        userB.setProviderId("twitter");
        assertThat(userA).isNotEqualTo(userB);
        assertThat(userA).isEqualTo(userAA);
        assertThat(userA.hashCode()).isEqualTo(userAA.hashCode());
        assertThat(userA.toString()).isNotBlank();
    }

    @Test
    @Transactional
    public void equalsVerifierTwitterProfile() throws Exception {
        TwitterProfile userA = new TwitterProfile(100, "", "", "", "");
        TwitterProfile userAA = new TwitterProfile(100, "", "", "", "");
        TwitterProfile userB = new TwitterProfile(200, "", "", "", "");
        assertThat(userA).isNotEqualTo(userB);
        assertThat(userA).isEqualTo(userAA);
        assertThat(userA.hashCode()).isEqualTo(userAA.hashCode());
        assertThat(userA.toString()).isNotBlank();
    }
}
