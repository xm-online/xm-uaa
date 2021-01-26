package com.icthh.xm.uaa.security.oauth2.idp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class reads and process both IDP clients public and private configuration for each tenant.
 * Tenant IDP clients created for each successfully loaded config. If config not fully loaded it skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdpConfigRepository implements RefreshableConfiguration {

    //FIXME logic for init DefinitionSourceLoader
    private static final String PUBLIC_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * In memory storage for storing information tenant IDP clients public/private configuration.
     * We need to store this information in memory cause public/private configuration could be loaded and processed in random order.
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig>> idpClientConfigs = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Object>> idpPublicConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage.
     * Stores information about tenant IDP clients public/private configuration that currently in process.
     * We need to store this information in memory cause:
     * - public/private configuration could be loaded and processed in random order.
     * - to avoid corruption previously registered in-memory tenant clients config
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig>> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    private void updateIdpConfigs(String configKey, String config) {
        String tenantKey = extractTenantKeyFromPath(configKey);

        processPublicConfiguration(tenantKey, configKey, config);

        boolean isClientConfigurationEmpty = CollectionUtils.isEmpty(tmpIdpClientConfigs.get(tenantKey));

        if (isClientConfigurationEmpty) {
            log.info("For tenant [{}] IDP client configs not specified. "
                + "Removing all previously registered IDP clients for tenant [{}]", tenantKey, tenantKey);
            return;
        }

        updateInMemoryConfig(tenantKey);
    }

    private void processPublicConfiguration(String tenantKey, String configKey, String config) {
        if (!matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return;
        }
        IdpPublicConfig idpPublicConfig = parseConfig(tenantKey, config, IdpPublicConfig.class);

        if (idpPublicConfig != null && idpPublicConfig.getConfig() != null) {
            idpPublicConfig
                .getConfig()
                .getClients()
                .forEach(publicIdpConf -> {
                        String idpConfKey = publicIdpConf.getKey();

                        Map<String, IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig> idpConfigContainers =
                            tmpIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>());
                        idpConfigContainers.put(idpConfKey, publicIdpConf);
                    }
                );

            Map<String, Object> publicConfig = new HashMap<>();

            //FIXME remove this stub after on uaa will be added jwks endpoint
            publicConfig.put("jwksSourceType", "remote");

            idpPublicConfigs.put(tenantKey, publicConfig);
        }
    }

    private <T> T parseConfig(String tenantKey, String config, Class<T> configType) {
        T parsedConfig = null;
        try {
            parsedConfig = objectMapper.readValue(config, configType);
        } catch (JsonProcessingException e) {
            log.error("Something went wrong during attempt to read {} for tenant:{}", config.getClass(), tenantKey, e);
        }
        return parsedConfig;
    }

    /**
     * <p>
     * Basing on input configuration method removes all previously registered clients for specified tenant
     * to avoid redundant clients registration presence
     * </p>
     *
     * @param tenantKey tenant key
     */
    private void updateInMemoryConfig(String tenantKey) {
        idpClientConfigs.put(tenantKey, tmpIdpClientConfigs.get(tenantKey));
        tmpIdpClientConfigs.remove(tenantKey);
    }

    private String extractTenantKeyFromPath(String configKey) {
        Map<String, String> configKeyParams =
            matcher.extractUriTemplateVariables(IdpConfigRepository.PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    public Map<String, IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig> getIdpClientConfigsByTenantKey(String tenantKey) {
        return idpClientConfigs.get(tenantKey);
    }

    public Map<String, Object> getIdpPublicConfigByTenantKey(String tenantKey) {
        return idpPublicConfigs.get(tenantKey);
    }
}
