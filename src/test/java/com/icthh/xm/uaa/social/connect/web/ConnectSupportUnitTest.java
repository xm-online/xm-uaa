package com.icthh.xm.uaa.social.connect.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.oauth1.OAuth1Operations;
import org.springframework.social.oauth1.OAuth1Version;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.NativeWebRequest;

public class ConnectSupportUnitTest {

    @InjectMocks
    private ConnectSupport target;

    @Mock
    private SessionStrategy sessionStrategy;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private OAuth1ConnectionFactory<?> oauth1ConnectionFactory;

    @Mock
    private Connection<?> connection;

    @Mock
    private OAuth1Operations oauth1Operations;

    @Mock
    private OAuthToken oauthToken;

    @Mock
    private OAuth2ConnectionFactory<?> oauth2ConnectionFactory;

    @Mock
    private OAuth2Operations oauth2Operations;

    @Mock
    private AccessGrant accessGrant;

    @Mock
    private NativeWebRequest request;

    @Mock
    private HttpServletRequest httpRequest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContext.setCurrent(new TenantInfo("tenant", "userLogin", "", "protocol", "domain", "port", ""));
        when(request.getNativeRequest(eq(HttpServletRequest.class))).thenReturn(httpRequest);
        when(oauth1ConnectionFactory.getOAuthOperations()).thenReturn(oauth1Operations);
        when(oauth2ConnectionFactory.getOAuthOperations()).thenReturn(oauth2Operations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildOuathUrlFactoryNotSupported() {
        String result = target.buildOAuthUrl(connectionFactory, request, new LinkedMultiValueMap<>());
    }

    @Test
    public void testBuildOuath1Url() {
        when(oauth1Operations.getVersion()).thenReturn(OAuth1Version.CORE_10);
        when(oauth1Operations.fetchRequestToken(any(), any())).thenReturn(oauthToken);
        when(oauth1Operations.buildAuthenticateUrl(any(), any())).thenReturn("oauth1redirect");

        String result = target.buildOAuthUrl(oauth1ConnectionFactory, request, new LinkedMultiValueMap<>());

        assertEquals("oauth1redirect", result);
    }

    @Test
    public void testBuildOuath2Url() {
        when(oauth2Operations.buildAuthenticateUrl(any())).thenReturn("oauth2redirect");

        String result = target.buildOAuthUrl(oauth2ConnectionFactory, request, new LinkedMultiValueMap<>());

        assertEquals("oauth2redirect", result);
    }

    @Test
    public void testCompleteConnectionOauth1() {
        when(oauth1Operations.exchangeForAccessToken(any(), any())).thenReturn(oauthToken);
        Mockito.<Connection<?>>when(oauth1ConnectionFactory.createConnection(eq(oauthToken))).thenReturn(connection);

        Connection<?> result = target.completeConnection(oauth1ConnectionFactory, request);

        assertEquals(connection, result);
    }

    @Test(expected = IllegalStateException.class)
    public void testCompleteConnectionOauth2InvalidState() {
        when(oauth2Operations.exchangeForAccess(any(), any(), any())).thenReturn(accessGrant);
        when(oauth2ConnectionFactory.supportsStateParameter()).thenReturn(true);
        Mockito.<Connection<?>>when(oauth2ConnectionFactory.createConnection(eq(accessGrant))).thenReturn(connection);

        Connection<?> result = target.completeConnection(oauth2ConnectionFactory, request);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testCompleteConnectionOauth2Exception() {
        when(oauth2Operations.exchangeForAccess(any(), any(), any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        when(oauth2ConnectionFactory.supportsStateParameter()).thenReturn(true);
        when(request.getParameter(eq("state"))).thenReturn("testState");
        when(sessionStrategy.getAttribute(eq(request), eq("oauth2State"))).thenReturn("testState");

        Connection<?> result = target.completeConnection(oauth2ConnectionFactory, request);
    }

    @Test
    public void testCompleteConnectionOauth2() {
        when(oauth2Operations.exchangeForAccess(any(), any(), any())).thenReturn(accessGrant);
        when(oauth2ConnectionFactory.supportsStateParameter()).thenReturn(true);
        when(request.getParameter(eq("state"))).thenReturn("testState");
        when(sessionStrategy.getAttribute(eq(request), eq("oauth2State"))).thenReturn("testState");
        Mockito.<Connection<?>>when(oauth2ConnectionFactory.createConnection(eq(accessGrant))).thenReturn(connection);

        Connection<?> result = target.completeConnection(oauth2ConnectionFactory, request);

        assertEquals(connection, result);
    }
}
