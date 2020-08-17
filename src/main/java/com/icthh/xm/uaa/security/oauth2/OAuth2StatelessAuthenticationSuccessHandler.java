package com.icthh.xm.uaa.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Optional.ofNullable;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;

@RequiredArgsConstructor
public class OAuth2StatelessAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AuthorizationServerTokenServices tokenServices;
    private final UserDetailsService userDetailsService;

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = auth.getPrincipal();
        String authorizedClientRegistrationId = auth.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient oAuth2AuthorizedClient =
            authorizedClientService.loadAuthorizedClient(authorizedClientRegistrationId, principal.getName());

        ofNullable(oAuth2AuthorizedClient.getAccessToken())
            .map(OAuth2AccessToken::getTokenValue)
            .ifPresent(token -> response.addCookie(createCookie("IDP_ACCESS_TOKEN", token)));

        ofNullable(oAuth2AuthorizedClient.getRefreshToken())
            .map(OAuth2RefreshToken::getTokenValue)
            .ifPresent(token -> response.addCookie(createCookie("IDP_REFRESH_TOKEN", token)));

        if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            ofNullable(oidcUser.getIdToken())
                .map(AbstractOAuth2Token::getTokenValue)
                .ifPresent(token -> response.addCookie(createCookie("IDP_ID_TOKEN", token)));
        }

        boolean isXmTokenEnabled = true; //todo get from config value
        if (isXmTokenEnabled) {
            org.springframework.security.oauth2.common.OAuth2AccessToken xmToken = createXmToken(principal);
            response.addCookie(createCookie("ACCESS_TOKEN", xmToken.getValue()));
            ofNullable(xmToken.getRefreshToken())
                .map(org.springframework.security.oauth2.common.OAuth2RefreshToken::getValue)
                .ifPresent(token -> response.addCookie(createCookie("REFRESH_TOKEN", token)));
        }
        HttpCookieOAuth2AuthorizationRequestRepository.deleteCookies(request, response);
        authorizedClientService.removeAuthorizedClient(authorizedClientRegistrationId, principal.getName());

        super.handle(request, response, authentication);
    }

    private org.springframework.security.oauth2.common.OAuth2AccessToken createXmToken(OAuth2User principal) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getAttribute(EMAIL));
        Authentication userAuth = new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities());
        OAuth2Request oAuth2Request = new OAuth2Request(null, "webapp", null, true, of("openid"), null, null, null, null);
        OAuth2Authentication userAuthentication = new OAuth2Authentication(oAuth2Request, userAuth);
        return tokenServices.createAccessToken(new OAuth2Authentication(oAuth2Request, userAuthentication));
    }


    private Cookie createCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(60);
        cookie.setPath("/");
        return cookie;
    }
}
