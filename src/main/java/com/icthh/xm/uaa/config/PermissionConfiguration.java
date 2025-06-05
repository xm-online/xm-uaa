package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.permission.service.PermissionMappingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermissionConfiguration {

    @Bean(name = "allPermissionMappingService")
    public PermissionMappingService allPermissionMappingService() {
        return new PermissionMappingService(p -> true);
    }
}
