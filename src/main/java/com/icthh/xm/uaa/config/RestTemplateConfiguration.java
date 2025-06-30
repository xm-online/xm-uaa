package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.oauth2.UaaClientAuthenticationHandler;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestTemplate;

import static com.icthh.xm.uaa.config.Constants.ACCESS_TOKEN_URL;
import static java.util.Collections.singletonList;

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
    private final LoadBalancerClient loadBalancerClient;

    @Value("${ribbon.http.client.enabled:true}")
    private Boolean ribbonTemplateEnabled;

    @Bean
    public RestTemplate loadBalancedRestTemplate(ObjectProvider<RestTemplateCustomizer> customizerProvider) {
        RestTemplate restTemplate = new RestTemplate();

        if (ribbonTemplateEnabled) {
            log.info("loadBalancedRestTemplate: using Ribbon load balancer");
            customizerProvider.ifAvailable(customizer -> customizer.customize(restTemplate));
        }

        return restTemplate;
    }

    @Bean
    public OAuth2RestTemplate oAuth2RestTemplate(ObjectProvider<RestTemplateCustomizer> customizerProvider) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setClientId(findClientId());
        resource.setClientSecret(applicationProperties.getDefaultClientSecret());
        resource.setAccessTokenUri(ACCESS_TOKEN_URL);

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resource, oauth2ClientContext);

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(applicationProperties.getConnectTimeoutMillis());
        simpleClientHttpRequestFactory.setReadTimeout(applicationProperties.getReadTimeoutMillis());
        oAuth2RestTemplate.setRequestFactory(simpleClientHttpRequestFactory);

        ClientCredentialsAccessTokenProvider accessTokenProvider = new ClientCredentialsAccessTokenProvider();
        accessTokenProvider.setAuthenticationHandler(uaaClientAuthenticationHandler);

        if (ribbonTemplateEnabled) {
            log.info("oAuth2RestTemplate: using Ribbon load balancer");
            LoadBalancerInterceptor loadBalancerInterceptor = new LoadBalancerInterceptor(loadBalancerClient);
            accessTokenProvider.setInterceptors(singletonList(loadBalancerInterceptor));
            customizerProvider.ifAvailable(customizer -> customizer.customize(oAuth2RestTemplate));
        }

        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProvider);

        return oAuth2RestTemplate;
    }

    private String findClientId() {
        String clientId = applicationProperties.getDefaultClientId().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Client id not found"));
        log.info("Property clientId: {}", clientId);
        return clientId;
    }

}
