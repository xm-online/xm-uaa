package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.oauth2.UaaClientAuthenticationHandler;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.client.RestTemplate;

import static com.icthh.xm.uaa.config.Constants.ACCESS_TOKEN_URL;

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
//    private final LoadBalancerClient loadBalancerClient;
    private final DiscoveryClient discoveryClient;
//    private final RemoteTokenServices remoteTokenServices;

    @Bean
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
//        remoteTokenServices.setRestTemplate(restTemplate);
        return restTemplate;
    }

    @Bean
    @DependsOn("tenantPropertiesService")
    public OAuth2RestTemplate oAuth2RestTemplate(RestTemplateCustomizer customizer) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setClientId(findClientId());
        resource.setClientSecret(tenantPropertiesService.getTenantProps().getSecurity().getDefaultClientSecret());
        resource.setAccessTokenUri(ACCESS_TOKEN_URL);

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resource, oauth2ClientContext);

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(applicationProperties.getConnectTimeoutMillis());
        simpleClientHttpRequestFactory.setReadTimeout(applicationProperties.getReadTimeoutMillis());
        oAuth2RestTemplate.setRequestFactory(simpleClientHttpRequestFactory);

        ClientCredentialsAccessTokenProvider accessTokenProvider = new ClientCredentialsAccessTokenProvider();
        accessTokenProvider.setAuthenticationHandler(uaaClientAuthenticationHandler);
        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProvider);

//        LoadBalancerInterceptor loadBalancerInterceptor = new LoadBalancerInterceptor(loadBalancerClient);
//        oAuth2RestTemplate.getInterceptors().add(loadBalancerInterceptor);

        customizer.customize(oAuth2RestTemplate);

//        remoteTokenServices.setRestTemplate(oAuth2RestTemplate);

        String uaa = discoveryClient.getInstances("uaa").stream().findFirst().get().getUri().toString();
        log.info("discoveryClient uri: {}", uaa);

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
