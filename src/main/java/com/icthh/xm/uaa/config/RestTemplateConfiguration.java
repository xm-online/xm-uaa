package com.icthh.xm.uaa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Configuration for RestTemplate beans.
 */
@Configuration
public class RestTemplateConfiguration {

    @Bean
    @Order(HIGHEST_PRECEDENCE)
    @ConditionalOnConsulEnabled
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate defaultRestTemplate() {
        return new RestTemplate();
    }
}
