package com.icthh.xm.uaa.social.connect.web;

import static java.util.Arrays.asList;

import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.NativeWebRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectSupport {

    private static final String OAUTH2_STATE_ATTRIBUTE = "oauth2State";

    private final XmRequestContextHolder xmRequestContextHolder;

    /**
     * Builds the provider URL to redirect the user to for connection authorization.
     *
     * @param connectionFactory the service provider's connection factory e.g. FacebookConnectionFactory
     * @param request the current web request
     * @param additionalParameters parameters to add to the authorization URL.
     * @return the URL to redirect the user to for authorization
     * @throws IllegalArgumentException if the connection factory is not OAuth1 based.
     */
    public String buildOAuthUrl(OAuth2ConnectionFactory<?> connectionFactory, NativeWebRequest request,
                                MultiValueMap<String, String> additionalParameters) {
        OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
        String defaultScope = connectionFactory.getScope();
        OAuth2Parameters parameters = getOAuth2Parameters(request, defaultScope, additionalParameters);
        String state = connectionFactory.generateState();
        parameters.add("state", state);
        return oauthOperations.buildAuthenticateUrl(parameters);
    }

    /**
     * Complete the connection to the OAuth2 provider.
     *
     * @param connectionFactory the service provider's connection factory e.g. FacebookConnectionFactory
     * @param request the current web request
     * @return a new connection to the service provider
     */
    public Connection completeConnection(OAuth2ConnectionFactory<?> connectionFactory, NativeWebRequest request) {
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

    protected String callbackUrl(NativeWebRequest request) {
        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        return "http://localhost:8080/google/login";
        // TODO
        //return UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/uaa" + connectPath(nativeRequest);
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
