package com.icthh.xm.uaa.security.oauth2.idp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class reads and process both IDP clients public and private configuration for each tenant.
 * Tenant IDP clients created for each successfully loaded config. If config not fully loaded it skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated //TODO remove it
public class IdpConfigRepository implements RefreshableConfiguration {

    //FIXME read only settings-public.yml + logic for init DefinitionSourceLoader
    private static final String PUBLIC_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String PRIVATE_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/idp-config.yml";
    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * In memory storage for storing information tenant IDP clients public/private configuration.
     * We need to store this information in memory cause public/private configuration could be loaded and processed in random order.
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpConfigContainer>> idpClientConfigs = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Object>> idpPublicConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage.
     * Stores information about tenant IDP clients public/private configuration that currently in process.
     * We need to store this information in memory cause:
     * - public/private configuration could be loaded and processed in random order.
     * - to avoid corruption previously registered in-memory tenant clients config
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpConfigContainer>> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage for storing information is tenant public/private configuration process state.
     * Generally speaking this information allows to understand is tenant public & private configuration loaded and processed.
     * We need to store this information in memory cause public/private configuration could be loaded and processed in random order.
     * Map key respond for tenant name, map value respond for config process state.
     * Left pair value relates to public config process state, right pair value relates to private config process state.
     */
    private final Map<String, MutablePair<Boolean, Boolean>> idpClientConfigProcessingState = new ConcurrentHashMap<>();

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, updatedKey)
            || matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    private void updateIdpConfigs(String configKey, String config) {
        String tenantKey = getTenantKey(configKey);

        processPublicConfiguration(tenantKey, configKey, config);

        processPrivateConfiguration(tenantKey, configKey, config);

        Map<String, IdpConfigContainer> idpConfigContainers =
            tmpIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>());

        Map<String, IdpConfigContainer> applicablyIdpConfigs = idpConfigContainers
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().isApplicable())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (CollectionUtils.isEmpty(applicablyIdpConfigs)) {
            MutablePair<Boolean, Boolean> configProcessingState = idpClientConfigProcessingState.get(tenantKey);

            boolean isPublicConfigProcessed = configProcessingState.getLeft() != null && configProcessingState.getLeft();
            boolean isPrivateConfigProcess = configProcessingState.getRight() != null && configProcessingState.getRight();
            boolean isClientConfigurationEmpty = CollectionUtils.isEmpty(tmpIdpClientConfigs.get(tenantKey));

            // if both public and private tenant configs processed
            // and client configuration not present at all then all tenant client registrations should be removed
            if (isPublicConfigProcessed && isPrivateConfigProcess && isClientConfigurationEmpty) {
                log.info("For tenant [{}] IDP client configs not specified. "
                    + "Removing all previously registered IDP clients for tenant [{}]", tenantKey, tenantKey);
                idpClientConfigProcessingState.remove(tenantKey);
            } else {
                log.info("For tenant [{}] IDP configs not fully loaded or it has lack of configuration", tenantKey);
            }

            return;
        }

        updateInMemoryConfig(tenantKey, applicablyIdpConfigs);
    }

    private String getTenantKey(String configKey) {
        if (matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return extractTenantKeyFromPath(configKey, PUBLIC_SETTINGS_CONFIG_PATH_PATTERN);
        } else {
            return extractTenantKeyFromPath(configKey, PRIVATE_SETTINGS_CONFIG_PATH_PATTERN);
        }
    }

    //TODO processPrivateConfiguration and  processPublicConfiguration very similar, think how to combine them
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

                        IdpConfigContainer idpConfigContainer = getIdpConfigContainer(tenantKey, idpConfKey);
                        idpConfigContainer.setIdpPublicClientConfig(publicIdpConf);
                    }
                );

            Map<String, Object> publicConfig = new HashMap<>();
            publicConfig.put("directLogin", idpPublicConfig.getConfig().isDirectLogin());

            if (idpPublicConfig.getConfig().getJwksSourceType() != null) {
                publicConfig.put("jwksSourceType", idpPublicConfig.getConfig().getJwksSourceType());
            }

            idpPublicConfigs.put(tenantKey, publicConfig);
        }

        MutablePair<Boolean, Boolean> configProcessingState =
            idpClientConfigProcessingState.computeIfAbsent(tenantKey, key -> new MutablePair<>());
        configProcessingState.setLeft(true);

    }

    private void processPrivateConfiguration(String tenantKey, String configKey, String config) {
        if (!matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return;
        }

        IdpPrivateConfig idpPrivateConfig = parseConfig(tenantKey, config, IdpPrivateConfig.class);

        if (idpPrivateConfig != null && idpPrivateConfig.getConfig() != null) {
            idpPrivateConfig
                .getConfig()
                .getClients()
                .forEach(privateIdpConf -> {
                        String idpConfKey = privateIdpConf.getKey();

                        IdpConfigContainer idpConfigContainer = getIdpConfigContainer(tenantKey, idpConfKey);
                        idpConfigContainer.setIdpPrivateClientConfig(privateIdpConf);
                    }
                );
        }

        MutablePair<Boolean, Boolean> configProcessingState =
            idpClientConfigProcessingState.computeIfAbsent(tenantKey, key -> new MutablePair<>());
        configProcessingState.setRight(true);
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
     * @param tenantKey         tenant key
     * @param applicablyConfigs fully loaded configs for processing
     */
    private void updateInMemoryConfig(String tenantKey, Map<String, IdpConfigContainer> applicablyConfigs) {
        tmpIdpClientConfigs.remove(tenantKey);
        idpClientConfigs.put(tenantKey, applicablyConfigs);
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        Map<String, String> configKeyParams = matcher.extractUriTemplateVariables(settingsConfigPath, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    private IdpConfigContainer getIdpConfigContainer(String tenantKey, String registrationId) {
        Map<String, IdpConfigContainer> idpConfigContainers =
            tmpIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>());

        return idpConfigContainers.computeIfAbsent(registrationId, key -> new IdpConfigContainer());
    }

    public Map<String, Map<String, IdpConfigContainer>> getAllIdpClientConfigs() {
        return idpClientConfigs;
    }

    public Map<String, IdpConfigContainer> getIdpClientConfigsByTenantKey(String tenantKey) {
        return idpClientConfigs.get(tenantKey);
    }

    public Map<String, Object> getIdpPublicConfigByTenantKey(String tenantKey) {
        return idpPublicConfigs.get(tenantKey);
    }
}
