package com.icthh.xm.uaa.security.social;

import com.google.common.collect.Sets;
import com.icthh.xm.uaa.config.tenant.TenantUtil;
import javax.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

public class CustomSignInAdapter implements SignInAdapter {

    private final UserDetailsService userDetailsService;

    private final AuthorizationServerTokenServices tokenServices;

    public CustomSignInAdapter(UserDetailsService userDetailsService, AuthorizationServerTokenServices tokenServices) {
        this.userDetailsService = userDetailsService;
        this.tokenServices = tokenServices;
    }

    @Override
    public String signIn(String userId, Connection<?> connection, NativeWebRequest request) {
        UserDetails user = userDetailsService.loadUserByUsername(connection.fetchUserProfile().getEmail());
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
            user,
            "N/A",
            user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(userAuth);

        String jwt = createToken(userAuth);
        ((ServletWebRequest) request).getResponse().addCookie(getSocialAuthenticationCookie(jwt));

        return TenantUtil.getApplicationUrl() + "/social-auth";
    }

    private String createToken(Authentication userAuth) {
        OAuth2Request storedOAuth2Request = new OAuth2Request(null, "web_app", null, true, Sets.newHashSet("openid"),
            null, null, null, null);
        OAuth2Authentication oauth2 = new OAuth2Authentication(storedOAuth2Request, userAuth);
        OAuth2AccessToken oauthToken = tokenServices.createAccessToken(oauth2);
        return oauthToken.getValue();
    }

    private static Cookie getSocialAuthenticationCookie(String token) {
        Cookie socialAuthCookie = new Cookie("social-authentication", token);
        // socialAuthCookie.setSecure(true);
        socialAuthCookie.setPath("/");
        socialAuthCookie.setMaxAge(60);
        return socialAuthCookie;
    }
}
