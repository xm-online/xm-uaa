package com.icthh.xm.uaa.social.connect.web;

import static java.util.Arrays.asList;

import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.oauth1.AuthorizedRequestToken;
import org.springframework.social.oauth1.OAuth1Operations;
import org.springframework.social.oauth1.OAuth1Parameters;
import org.springframework.social.oauth1.OAuth1Version;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides common connect support and utilities for Java web/servlet environments.
 */
@Slf4j
public class ConnectSupport {

    private static final String OAUTH_TOKEN_ATTRIBUTE = "oauthToken";

    private static final String OAUTH2_STATE_ATTRIBUTE = "oauth2State";

    private final SessionStrategy sessionStrategy;
    private final XmRequestContextHolder xmRequestContextHolder;

    public ConnectSupport(SessionStrategy sessionStrategy,
                          XmRequestContextHolder xmRequestContextHolder) {
        this.sessionStrategy = sessionStrategy;
        this.xmRequestContextHolder = xmRequestContextHolder;
    }

    /**
     * Builds the provider URL to redirect the user to for connection authorization.
     *
     * @param connectionFactory the service provider's connection factory e.g. FacebookConnectionFactory
     * @param request the current web request
     * @param additionalParameters parameters to add to the authorization URL.
     * @return the URL to redirect the user to for authorization
     * @throws IllegalArgumentException if the connection factory is not OAuth1 based.
     */
    public String buildOAuthUrl(ConnectionFactory<?> connectionFactory, NativeWebRequest request,
        MultiValueMap<String, String> additionalParameters) {
        if (connectionFactory instanceof OAuth1ConnectionFactory) {
            return buildOAuth1Url((OAuth1ConnectionFactory<?>) connectionFactory, request, additionalParameters);
        } else if (connectionFactory instanceof OAuth2ConnectionFactory) {
            return buildOAuth2Url((OAuth2ConnectionFactory<?>) connectionFactory, request, additionalParameters);
        } else {
            throw new IllegalArgumentException("ConnectionFactory not supported");
        }
    }

    /**
     * Complete the connection to the OAuth1 provider.
     *
     * @param connectionFactory the service provider's connection factory e.g. FacebookConnectionFactory
     * @param request the current web request
     * @return a new connection to the service provider
     */
    public Connection completeConnection(OAuth1ConnectionFactory<?> connectionFactory, NativeWebRequest request) {
        String verifier = request.getParameter("oauth_verifier");
        AuthorizedRequestToken requestToken = new AuthorizedRequestToken(extractCachedRequestToken(request), verifier);
        OAuthToken accessToken = connectionFactory.getOAuthOperations().exchangeForAccessToken(requestToken, null);
        return connectionFactory.createConnection(accessToken);
    }

    /**
     * Complete the connection to the OAuth2 provider.
     *
     * @param connectionFactory the service provider's connection factory e.g. FacebookConnectionFactory
     * @param request the current web request
     * @return a new connection to the service provider
     */
    public Connection completeConnection(OAuth2ConnectionFactory<?> connectionFactory, NativeWebRequest request) {
        if (connectionFactory.supportsStateParameter()) {
            verifyStateParameter(request);
        }

        String code = request.getParameter("code");
        try {
            AccessGrant accessGrant = connectionFactory.getOAuthOperations()
                .exchangeForAccess(code, callbackUrl(request), null);
            return connectionFactory.createConnection(accessGrant);
        } catch (HttpClientErrorException e) {
            log.warn("HttpClientErrorException while completing connection: " + e.getMessage());
            log.warn("      Response body: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private void verifyStateParameter(NativeWebRequest request) {
        String state = request.getParameter("state");
        String originalState = extractCachedOAuth2State(request);
        if (state == null || !state.equals(originalState)) {
            throw new IllegalStateException("The OAuth2 'state' parameter is missing or doesn't match.");
        }
    }

    protected String callbackUrl(NativeWebRequest request) {
        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        return UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/uaa" + connectPath(nativeRequest);
    }

    private String buildOAuth1Url(OAuth1ConnectionFactory<?> connectionFactory, NativeWebRequest request,
        MultiValueMap<String, String> additionalParameters) {
        OAuth1Operations oauthOperations = connectionFactory.getOAuthOperations();
        MultiValueMap<String, String> requestParameters = getRequestParameters(request);
        OAuth1Parameters parameters = getOAuth1Parameters(request, additionalParameters);
        parameters.putAll(requestParameters);
        if (oauthOperations.getVersion() == OAuth1Version.CORE_10) {
            parameters.setCallbackUrl(callbackUrl(request));
        }
        OAuthToken requestToken = fetchRequestToken(request, requestParameters, oauthOperations);
        sessionStrategy.setAttribute(request, OAUTH_TOKEN_ATTRIBUTE, requestToken);
        return buildOAuth1Url(oauthOperations, requestToken.getValue(), parameters);
    }

    private static String buildOAuth1Url(OAuth1Operations oauthOperations, String requestToken,
        OAuth1Parameters parameters) {
        return oauthOperations.buildAuthenticateUrl(requestToken, parameters);
    }

    private OAuth1Parameters getOAuth1Parameters(NativeWebRequest request,
        MultiValueMap<String, String> additionalParameters) {
        OAuth1Parameters parameters = new OAuth1Parameters(additionalParameters);
        parameters.putAll(getRequestParameters(request));
        return parameters;
    }

    private OAuthToken fetchRequestToken(NativeWebRequest request, MultiValueMap<String, String> requestParameters,
        OAuth1Operations oauthOperations) {
        if (oauthOperations.getVersion() == OAuth1Version.CORE_10_REVISION_A) {
            return oauthOperations.fetchRequestToken(callbackUrl(request), requestParameters);
        }
        return oauthOperations.fetchRequestToken(null, requestParameters);
    }

    private String buildOAuth2Url(OAuth2ConnectionFactory<?> connectionFactory, NativeWebRequest request,
        MultiValueMap<String, String> additionalParameters) {
        OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
        String defaultScope = connectionFactory.getScope();
        OAuth2Parameters parameters = getOAuth2Parameters(request, defaultScope, additionalParameters);
        String state = connectionFactory.generateState();
        parameters.add("state", state);
        sessionStrategy.setAttribute(request, OAUTH2_STATE_ATTRIBUTE, state);
        return oauthOperations.buildAuthenticateUrl(parameters);
    }

    private OAuth2Parameters getOAuth2Parameters(NativeWebRequest request, String defaultScope,
        MultiValueMap<String, String> additionalParameters) {
        OAuth2Parameters parameters = new OAuth2Parameters(additionalParameters);
        parameters.putAll(getRequestParameters(request, "scope"));
        parameters.setRedirectUri(callbackUrl(request));
        String scope = request.getParameter("scope");
        if (scope != null) {
            parameters.setScope(scope);
        } else if (defaultScope != null) {
            parameters.setScope(defaultScope);
        }
        return parameters;
    }

    private static String connectPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo != null ? pathInfo : "");
    }

    private OAuthToken extractCachedRequestToken(WebRequest request) {
        OAuthToken requestToken = (OAuthToken) sessionStrategy.getAttribute(request, OAUTH_TOKEN_ATTRIBUTE);
        sessionStrategy.removeAttribute(request, OAUTH_TOKEN_ATTRIBUTE);
        return requestToken;
    }

    private String extractCachedOAuth2State(WebRequest request) {
        String state = (String) sessionStrategy.getAttribute(request, OAUTH2_STATE_ATTRIBUTE);
        sessionStrategy.removeAttribute(request, OAUTH2_STATE_ATTRIBUTE);
        return state;
    }

    private static MultiValueMap<String, String> getRequestParameters(NativeWebRequest request,
        String... ignoredParameters) {
        List<String> ignoredParameterList = asList(ignoredParameters);
        MultiValueMap<String, String> convertedMap = new LinkedMultiValueMap<>();
        for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (!ignoredParameterList.contains(entry.getKey())) {
                convertedMap.put(entry.getKey(), asList(entry.getValue()));
            }
        }
        return convertedMap;
    }

}
