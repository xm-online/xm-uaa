package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantLogins;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantLoginsService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, TenantLogins> loginProps = new ConcurrentHashMap<>();

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ApplicationProperties applicationProperties;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Get user logins properties.
     *
     * @return login props
     */
    public TenantLogins getLogins() {
        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!loginProps.containsKey(cfgTenantKey)) {
            throw new IllegalArgumentException("Tenant user logins configuration not found");
        }
        return loginProps.get(cfgTenantKey);
    }

    /**
     * Update user logins properties.
     *
     * @param loginsYml logins config yml
     */
    @SneakyThrows
    public void updateLogins(String loginsYml) {
        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String configName = applicationProperties.getTenantLoginPropertiesName();

        // Simple validation correct structure
        mapper.readValue(loginsYml, TenantLogins.class);

        tenantConfigRepository.updateConfig(tenantKey.toUpperCase(), "/" + configName, loginsYml);
    }

    @Override
    public void onRefresh(String key, String config) {
        String pathPattern = applicationProperties.getTenantLoginPropertiesPathPattern();
        try {
            // tenant in uppercase
            String tenant = matcher.extractUriTemplateVariables(pathPattern, key).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                loginProps.remove(tenant);
                log.info("Specification for tenant {} was removed", tenant);
            } else {
                TenantLogins spec = mapper.readValue(config, TenantLogins.class);
                loginProps.put(tenant, spec);
                log.info("Specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read specification from path " + key, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String key) {
        return matcher.match(applicationProperties.getTenantLoginPropertiesPathPattern(), key);
    }

    @Override
    public void onInit(String key, String config) {
        onRefresh(key, config);
    }
}
