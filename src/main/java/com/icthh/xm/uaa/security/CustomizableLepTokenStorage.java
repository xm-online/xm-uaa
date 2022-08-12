package com.icthh.xm.uaa.security;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface CustomizableLepTokenStorage {
    OAuth2AccessToken readAccessToken(String tokenValue);

    OAuth2Authentication readAuthentication(OAuth2AccessToken token);

    OAuth2RefreshToken readRefreshToken(String tokenValue);
}
