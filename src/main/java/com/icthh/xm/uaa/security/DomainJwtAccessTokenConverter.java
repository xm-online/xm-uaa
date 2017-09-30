package com.icthh.xm.uaa.security;

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.config.Constants.*;

/**
 * Overrides to add and get token tenant.
 */
public class DomainJwtAccessTokenConverter extends JwtAccessTokenConverter {

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        @SuppressWarnings("unchecked") final Map<String, Object> authDetails = (Map<String, Object>) authentication
            .getDetails();
        if (authDetails != null) {
            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(authDetails);
        }
        return super.enhance(accessToken, authentication);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        final OAuth2Authentication authentication = super.extractAuthentication(map);
        final Map<String, String> details = new HashMap<>();
        details.put(AUTH_TENANT_KEY, (String) map.get(AUTH_TENANT_KEY));
        details.put(AUTH_USER_KEY, (String) map.get(AUTH_USER_KEY));
        details.put(AUTH_USER_KEY, (String) map.get(AUTH_USER_KEY));
        authentication.setDetails(details);

        return authentication;
    }
}
