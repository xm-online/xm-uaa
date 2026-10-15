package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Domains of the tenant from /config/tenants/tenant-domains.yml.
 */
@Slf4j
@Service
public class TenantDomainsService implements RefreshableConfiguration {

    public static final String TENANT_DOMAINS_CONFIG_KEY = "/config/tenants/tenant-domains.yml";

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private volatile Map<String, Set<String>> domainsByTenant = Map.of();

    /**
     * Get domains of the tenant in lower case.
     *
     * @param tenantKey tenant key
     * @return domains, empty when nothing is configured for the tenant
     */
    public Set<String> getDomains(TenantKey tenantKey) {
        return domainsByTenant.getOrDefault(tenantKey.getValue(), Set.of());
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            Map<String, List<String>> domains = StringUtils.isBlank(config) ? null
                : mapper.readValue(config, new TypeReference<Map<String, List<String>>>() { });
            Map<String, Set<String>> result = new HashMap<>();
            if (domains != null) {
                domains.forEach((tenant, hosts) -> result.put(tenant.toUpperCase(),
                    hosts == null ? Set.of() : hosts.stream().map(String::toLowerCase).collect(toSet())));
            }
            domainsByTenant = result;
            log.info("Tenant domains config {} was updated: {}", updatedKey, result);
        } catch (Exception e) {
            log.error("Error read tenant domains config {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANT_DOMAINS_CONFIG_KEY.equals(updatedKey);
    }
}
