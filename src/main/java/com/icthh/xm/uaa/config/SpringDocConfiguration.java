package com.icthh.xm.uaa.config;

import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Optional.ofNullable;

@Configuration
public class SpringDocConfiguration {

    @Bean
    public ServerBaseUrlCustomizer serverBaseUrlRequestCustomizer() {
        return (serverBaseUrl, request) ->
            ofNullable(request.getHeaders().getFirst("X-Forwarded-Prefix")).orElse(serverBaseUrl);
    }
}
