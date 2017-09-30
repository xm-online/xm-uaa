package com.icthh.xm.uaa.social.connect.web;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Helper methods that support provider user sign-in scenarios.
 */
public class ProviderSignInUtils {

    private SessionStrategy sessionStrategy;
    private ConnectionFactoryLocator connectionFactoryLocator;

    public ProviderSignInUtils(SessionStrategy sessionStrategy, ConnectionFactoryLocator connectionFactoryLocator) {
        this.sessionStrategy = sessionStrategy;
        this.connectionFactoryLocator = connectionFactoryLocator;
    }

    /**
     * Get the connection to the provider user the client attempted to sign-in as. Using this connection you may fetch a
     * {@link Connection#fetchUserProfile() provider user profile} and use that to pre-populate a local user
     * registration/signup form. You can also lookup the id of the provider and use that to display a provider-specific
     * user-sign-in-attempt flash message e.g. "Your Facebook Account is not connected to a Local account. Please sign
     * up." Must be called before handlePostSignUp() or else the sign-in attempt will have been cleared from the
     * session. Returns null if no provider sign-in has been attempted for the current user session.
     *
     * @param request the current request attributes, used to extract sign-in attempt information from the current user
     * session
     * @return the connection
     */
    public Connection getConnectionFromSession(RequestAttributes request) {
        ProviderSignInAttempt signInAttempt = getProviderUserSignInAttempt(request);
        return signInAttempt != null ? signInAttempt.getConnection(connectionFactoryLocator) : null;
    }

    private ProviderSignInAttempt getProviderUserSignInAttempt(RequestAttributes request) {
        return (ProviderSignInAttempt) sessionStrategy.getAttribute(request, ProviderSignInAttempt.SESSION_ATTRIBUTE);
    }

}
