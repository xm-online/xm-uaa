package com.icthh.xm.uaa.social.connect.web;

import java.io.Serializable;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;

/**
 * Connects social user to local account.
 */
public class ProviderSignInAttempt implements Serializable {

    /**
     * Name of the session attribute ProviderSignInAttempt instances are indexed under.
     */
    public static final String SESSION_ATTRIBUTE = ProviderSignInAttempt.class.getName();

    private final ConnectionData connectionData;

    public ProviderSignInAttempt(Connection<?> connection) {
        this.connectionData = connection.createData();
    }

    /**
     * Get the connection to the provider user account the client attempted to sign-in as. Using this connection you may
     * fetch a {@link Connection#fetchUserProfile() provider user profile} and use that to pre-populate a local user
     * registration/signup form. You can also lookup the id of the provider and use that to display a provider-specific
     * user-sign-in-attempt flash message e.g. "Your Facebook Account is not connected to a Local account. Please sign
     * up."
     *
     * @param connectionFactoryLocator A {@link ConnectionFactoryLocator} used to lookup the connection
     * @return the connection
     */
    public Connection getConnection(ConnectionFactoryLocator connectionFactoryLocator) {
        return connectionFactoryLocator.getConnectionFactory(connectionData.getProviderId())
            .createConnection(connectionData);
    }

    /**
     * Connect the new local user to the provider.
     *
     * @param userId the local user ID
     * @param connectionFactoryLocator A {@link ConnectionFactoryLocator} used to lookup the connection
     * @param connectionRepository a {@link UsersConnectionRepository}
     * @throws DuplicateConnectionException if the user already has this connection
     */
    void addConnection(String userId, ConnectionFactoryLocator connectionFactoryLocator,
        UsersConnectionRepository connectionRepository) {
        connectionRepository.createConnectionRepository(userId).addConnection(getConnection(connectionFactoryLocator));
    }

}
