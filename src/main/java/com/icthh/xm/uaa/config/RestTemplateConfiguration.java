package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.oauth2.UaaClientAuthenticationHandler;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate beans.
 */
@Slf4j
@Configuration
@EnableOAuth2Client
@RequiredArgsConstructor
public class RestTemplateConfiguration {

    private final ApplicationProperties applicationProperties;
    private final TenantPropertiesService tenantPropertiesService;
    private final OAuth2ClientContext oauth2ClientContext;
    private final UaaClientAuthenticationHandler uaaClientAuthenticationHandler;

    @Bean
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    public OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();

        resource.setClientId(findClientId());
        resource.setClientSecret(tenantPropertiesService.getTenantProps().getSecurity().getDefaultClientSecret());
        resource.setAccessTokenUri(applicationProperties.getAccessTokenUrl());

        return resource;
    }

    @Bean
    public OAuth2RestTemplate oAuth2RestTemplate() {
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(oAuth2ProtectedResourceDetails(), oauth2ClientContext);

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(applicationProperties.getConnectTimeoutMillis());
        simpleClientHttpRequestFactory.setReadTimeout(applicationProperties.getReadTimeoutMillis());
        restTemplate.setRequestFactory(simpleClientHttpRequestFactory);

        ClientCredentialsAccessTokenProvider accessTokenProvider = new ClientCredentialsAccessTokenProvider();
        accessTokenProvider.setAuthenticationHandler(uaaClientAuthenticationHandler);

        restTemplate.setAccessTokenProvider(accessTokenProvider);

        return restTemplate;
    }

    private String findClientId() {
        String clientId = applicationProperties.getDefaultClientId().stream()
            .reduce((first, second) -> second).orElse(null);
        log.info("Property clientId: {}", clientId);
        return clientId;
    }

}
