package com.icthh.xm.uaa.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Logs in or registers a user after OpenID Connect SignIn/Up
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class XmOidcUserService extends OidcUserService {

    private final XmOAuth2UserService oauth2UserService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        oauth2UserService.createUserIfNotRegistered(oidcUser, userRequest.getClientRegistration().getRegistrationId());
        return oidcUser;
    }
}
