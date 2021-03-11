package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.UserSpec;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserSpecService implements RefreshableConfiguration {

    private final ConcurrentMap<String, Map<String, UserSpec>> userSpecs = new ConcurrentHashMap<>();
    private final ApplicationProperties applicationProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final TenantContextHolder tenantContextHolder;

    {
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    @LoggingAspectConfig(inputExcludeParams = "config")
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenantPropertiesPathPattern = applicationProperties.getTenantPropertiesPathPattern();
        try {
            String tenant = matcher.extractUriTemplateVariables(tenantPropertiesPathPattern, updatedKey).get("tenantName");
            if (isBlank(config)) {
                userSpecs.remove(tenant);
                return;
            }
            TenantProperties tenantProperties = parseProperties(config);
            userSpecs.put(tenant, toTypeSpecsMap(tenantProperties.getUserSpec()));
            log.info("Specification was for tenant {} updated", tenant);
        } catch (Exception e) {
            log.error("Error read specification from path " + updatedKey, e);
        }
    }

    @SneakyThrows
    protected TenantProperties parseProperties(String config) {
        return mapper.readValue(config, TenantProperties.class);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(applicationProperties.getTenantPropertiesPathPattern(), updatedKey);
    }

    public List<UserSpec> getUserSpec(List<String> roleKeys) {
        Map<String, UserSpec> spec = userSpecs.get(getTenantKeyValue());
        return spec != null ?
            roleKeys
                .stream()
                .map(spec::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()) :
            emptyList();
    }

    private Map<String, UserSpec> toTypeSpecsMap(List<UserSpec> userSpecs) {
        if (Objects.isNull(userSpecs)) {
            return Collections.emptyMap();
        } else {
            return userSpecs.stream()
                .collect(toMap(UserSpec::getRoleKey, identity()));
        }
    }

    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }
}
