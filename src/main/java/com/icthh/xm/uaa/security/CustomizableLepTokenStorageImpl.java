package com.icthh.xm.uaa.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomizableLepTokenStorageImpl implements CustomizableLepTokenStorage {

    private final JwtTokenStore jwtTokenStore;

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        return jwtTokenStore.readAccessToken(tokenValue);
    }

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        return jwtTokenStore.readAuthentication(token);
    }
}
