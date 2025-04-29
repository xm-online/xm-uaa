package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPrivilegesRefreshableConfiguration implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final String configPath = "/config/tenants/{tenantName}/context-privileges.yml";

    // tenant -> serviceName -> privileges collection
    private ConcurrentHashMap<String, Map<String, List<String>>> privileges = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final TenantContextHolder tenantContextHolder;

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            String tenant = matcher.extractUriTemplateVariables(configPath, updatedKey).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                privileges.remove(tenant);
                log.info("Context privileges for tenant {} was removed", tenant);
            } else {
                privileges.put(tenant, ymlToPrivileges(config));
                log.info("Context privileges for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error when read context privileges specification from path {}", updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(configPath, updatedKey);
    }

    public Map<String, List<String>> getPrivileges() {
        String tenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder.getContext());
        return privileges.getOrDefault(tenantName, Collections.emptyMap());
    }

    private Map<String, List<String>> ymlToPrivileges(String yml) {
        log.debug("Read custom-privileges config content: {}", yml);
        try {
            JsonNode root = mapper.readTree(yml);

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(root.fieldNames(), 0), false)
                .collect(Collectors.toUnmodifiableMap(
                    field -> field,
                    field -> StreamSupport.stream(root.get(field).spliterator(), false)
                        .map(node -> node.path("key").asText(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                ));
        } catch (Exception e) {
            log.error("Failed to read privileges collection from YML file, error: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
}
