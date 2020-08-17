package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.icthh.xm.uaa.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.icthh.xm.uaa.security.oauth2.OAuth2StatelessAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

@Configuration
public class OidcConfiguration {

    @Bean
    public OAuth2StatelessAuthenticationSuccessHandler authenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
                                                                                    AuthorizationServerTokenServices tokenServices,
                                                                                    UserDetailsService userDetailsService) {
        return new OAuth2StatelessAuthenticationSuccessHandler(authorizedClientService, tokenServices, userDetailsService);
    }

    @Bean
    public OAuth2AuthenticationFailureHandler authenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> httpCookieOAuth2AuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }
}
