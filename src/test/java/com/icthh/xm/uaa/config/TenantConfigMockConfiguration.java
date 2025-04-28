package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import org.springframework.core.env.Environment;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class TenantConfigMockConfiguration {

    @Bean
    public TenantListRepository tenantListRepository(Environment env) {
        Set<String> tenants = new HashSet<>();
        tenants.add("XM");
        if (!asList(env.getActiveProfiles()).contains("oracle-test")) {
            tenants.add("DEMO");
        }
        TenantListRepository mockTenantListRepository = mock(TenantListRepository.class);
        doAnswer(mvc -> tenants.add(mvc.getArguments()[0].toString())).when(mockTenantListRepository).addTenant(any());
        doAnswer(mvc -> tenants.remove(mvc.getArguments()[0].toString())).when(mockTenantListRepository).deleteTenant(any());
        when(mockTenantListRepository.getTenants()).thenReturn(tenants);
        return  mockTenantListRepository;
    }

    @Bean
    public TenantConfigRepository tenantConfigRepository() {
        return mock(TenantConfigRepository.class);
    }

    @Bean
    public TenantConfigService tenantConfigService() {
        return mock(TenantConfigService.class);
    }

    @Bean
    public CommonConfigRepository commonConfigRepository(){
        return mock(CommonConfigRepository.class);
    }
}
