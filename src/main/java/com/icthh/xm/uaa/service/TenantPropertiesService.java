package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPropertiesService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private ConcurrentHashMap<String, TenantProperties> tenantProps = new ConcurrentHashMap<>();

    private final AntPathMatcher matcher = new AntPathMatcher();

    private final ApplicationProperties applicationProperties;

    private final TenantConfigRepository tenantConfigRepository;

    public TenantProperties getTenantProps() {
        String tenant = TenantContext.getCurrent().getTenant();
        if (!tenantProps.containsKey(tenant)) {
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return tenantProps.get(tenant);
    }

    @SneakyThrows
    public void updateTenantProps(String timelineYml) {
        String tenant = TenantContext.getCurrent().getTenant();
        String configName = applicationProperties.getTenantPropertiesName();

        // Simple validation correct structure
        mapper.readValue(timelineYml, TenantProperties.class);

        tenantConfigRepository.updateConfig(tenant, "/" + configName, timelineYml);
    }

    @Override
    @SneakyThrows
    public void onRefresh(String updatedKey, String config) {
        String specificationPathPattern = applicationProperties.getTenantPropertiesPathPattern();
        try {
            String tenant = matcher.extractUriTemplateVariables(specificationPathPattern, updatedKey).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                tenantProps.remove(tenant);
                log.info("Specification for tenant {} was removed", tenant);
            } else {
                TenantProperties spec = mapper.readValue(config, TenantProperties.class);
                tenantProps.put(tenant, spec);
                log.info("Specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getTenantPropertiesPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
