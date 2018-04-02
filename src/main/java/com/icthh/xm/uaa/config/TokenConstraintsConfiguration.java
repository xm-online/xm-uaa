package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link TokenConstraintsConfiguration} class.
 */
@RequiredArgsConstructor
@Configuration
public class TokenConstraintsConfiguration {

    private final ApplicationProperties applicationProperties;
    private final TenantPropertiesService tenantPropertiesService;

    @Bean
    public TokenConstraintsService tokenConstraintsServices() {
        TokenConstraintsService tokenConstraints = new TokenConstraintsService();
        tokenConstraints.setSupportRefreshToken(true);
        tokenConstraints.setApplicationProperties(applicationProperties);
        tokenConstraints.setTenantPropertiesService(tenantPropertiesService);
        return tokenConstraints;
    }

}
