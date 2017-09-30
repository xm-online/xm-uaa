package com.icthh.xm.uaa.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public interface AuthenticationRefreshProvider {

    Authentication refresh(OAuth2Authentication authentication);
}
