package com.icthh.xm.uaa.social.twitter.api.impl;

import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;

/**
 * A utility implementation of an OAuth2 API binding used by TwitterTemplate to create an OAuth2
 * RestTemplate for client-authorized API requests. This is to handle the unique situation where
 * the API binding needs to use OAuth 1.0a for user requests and OAuth 2 for client requests.
 *
 * @author Craig Walls
 */
class ClientAuthorizedTwitterTemplate extends AbstractOAuth2ApiBinding {

    public ClientAuthorizedTwitterTemplate(String clientToken) {
        super(clientToken);
    }

}
