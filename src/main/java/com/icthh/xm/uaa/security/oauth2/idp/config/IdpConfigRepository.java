package com.icthh.xm.uaa.security.oauth2.idp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.idp.IdpConfigUtils;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.security.oauth2.idp.validation.verifiers.AudienceClaimVerifier;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.token.store.IssuerClaimVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;

/**
 * This class reads and process IDP clients public configuration for each tenant.
 * Tenant IDP clients created for each successfully loaded and valid config. If config not present or invalid it skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * In memory storage for storing information tenant IDP clients public.
     * idpClientConfigs structure Map< tenantKey, Map< clientKey, IdpPublicClientConfig>> where:
     * <ul>
     *   <li/> tenantKey - tenant id
     *   <li/> clientKey - client id
     *   <li/> IdpPublicClientConfig - tenant specific idp public client Config
     * </ul>
     */
    private final Map<String, Map<String, IdpPublicClientConfig>> idpClientConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage.
     * Stores information about tenant IDP clients public configuration that currently in process.
     * <p/>
     * We need to store this information in memory to avoid corruption previously registered in-memory tenant clients config
     * tmpIdpClientPublicConfigs structure Map< tenantKey, Map< clientKey, IdpPublicClientConfig>> where:
     * <ul>
     *   <li/> tenantKey - tenant id
     *   <li/> clientKey - client id
     *   <li/> IdpPublicClientConfig - tenant specific idp public client Config
     * </ul>
     */
    private final Map<String, Map<String, IdpPublicClientConfig>> tmpValidIdpClientPublicConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage.
     * Stores information about tenant IDP jwt claims verifiers.
     * <p/>
     * We need to store this information in memory to avoid initialization on each token claims verification
     * and store actual verifiers.
     * <p/>
     * jwtClaimsSetVerifiersHolder structure Map< tenantKey, Map< clientKey, List< JwtClaimsSetVerifier >>> where:
     * <ul>
     *   <li/> tenantKey - tenant id
     *   <li/> clientKey - client id
     *   <li/> List< JwtClaimsSetVerifier> - list of default token claim verifiers
     * </ul>
     */
    private final Map<String, Map<String, List<JwtClaimsSetVerifier>>> jwtClaimsSetVerifiersHolder = new ConcurrentHashMap<>();

    private final TenantContextHolder tenantContextHolder;

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    private void updateIdpConfigs(String configKey, String config) {
        String tenantKey = extractTenantKeyFromPath(configKey);

        List<IdpPublicClientConfig> rawIdpPublicClientConfigs =
            processPublicClientsConfiguration(tenantKey, configKey, config);

        boolean isRawClientsConfigurationEmpty = CollectionUtils.isEmpty(rawIdpPublicClientConfigs);
        boolean isValidClientsConfigurationEmpty = CollectionUtils.isEmpty(tmpValidIdpClientPublicConfigs.get(tenantKey));

        if (isRawClientsConfigurationEmpty && isValidClientsConfigurationEmpty) {
            log.warn("For tenant [{}] provided IDP public client configs not present." +
                "Removing client configs and claim verifiers from storage", tenantKey);
            idpClientConfigs.remove(tenantKey);
            jwtClaimsSetVerifiersHolder.remove(tenantKey);
            return;
        }
        if (isValidClientsConfigurationEmpty) {
            log.warn("For tenant [{}] provided IDP public client configs not applied.", tenantKey);
            return;
        }

        updateInMemoryClientConfig(tenantKey);
        buildJwtClaimsSetVerifiers(tenantKey);
    }

    private List<IdpPublicClientConfig> processPublicClientsConfiguration(String tenantKey, String configKey, String config) {
        log.debug("Processing public config for tenant [{}]: configKey {}, config {}", tenantKey, configKey, config);
        if (!matcher.match(IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return Collections.emptyList();
        }
        IdpPublicConfig idpPublicConfig = parseConfig(tenantKey, config, IdpPublicConfig.class);

        List<IdpPublicClientConfig> rawIdpPublicClientConfigs = Optional.ofNullable(idpPublicConfig)
            .map(IdpPublicConfig::getConfig)
            .map(IdpPublicConfig.IdpConfigContainer::getClients)
            .orElseGet(Collections::emptyList);

        rawIdpPublicClientConfigs
            .stream()
            .filter(IdpConfigUtils::isPublicClientConfigValid)
            .forEach(publicIdpConf -> putTmpIdpPublicConfig(tenantKey, publicIdpConf));
        return rawIdpPublicClientConfigs;
    }

    private void putTmpIdpPublicConfig(final String tenantKey, final IdpPublicClientConfig publicIdpConf) {
        tmpValidIdpClientPublicConfigs
            .computeIfAbsent(tenantKey, key -> new HashMap<>())
            .put(publicIdpConf.getKey(), publicIdpConf);
    }

    private <T> T parseConfig(String tenantKey, String config, Class<T> configType) {
        T parsedConfig = null;
        try {
            parsedConfig = objectMapper.readValue(config, configType);
        } catch (JsonProcessingException e) {
            log.error("Error occurred during attempt to read idp configuration {} for tenant:{}", config.getClass(), tenantKey, e);
        }
        return parsedConfig;
    }

    private String extractTenantKeyFromPath(String configKey) {
        Map<String, String> configKeyParams =
            matcher.extractUriTemplateVariables(IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    Map<String, IdpPublicClientConfig> getIdpClientConfigsByTenantKey(String tenantKey) {
        return idpClientConfigs.get(tenantKey);
    }

    /**
     * <p>
     * Basing on input configuration method removes all previously registered clients for specified tenant
     * to avoid redundant clients registration presence
     * </p>
     *  @param tenantKey         tenant key
     *
     */
    private void updateInMemoryClientConfig(String tenantKey) {
        idpClientConfigs.put(tenantKey, tmpValidIdpClientPublicConfigs.remove(tenantKey));
    }

    private void buildJwtClaimsSetVerifiers(String tenantKey) {
        Map<String, IdpPublicClientConfig> configs = getIdpClientConfigsByTenantKey(tenantKey);

        if (CollectionUtils.isEmpty(configs)) {
            return;
        }

        Map<String, List<JwtClaimsSetVerifier>> verifiers =
            configs.values()
                .stream()
                .map(this::getJwtClaimsSetVerifiers)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        log.debug("Register claim verifiers for tenant [{}]. Verifiers [{}]", tenantKey, verifiers);
        jwtClaimsSetVerifiersHolder.put(tenantKey, verifiers);
    }

    @SneakyThrows
    public List<JwtClaimsSetVerifier> getJwtClaimsSetVerifiers(String clientId) {
        String tenantKey = tenantContextHolder.getTenantKey();

        List<JwtClaimsSetVerifier> jwtClaimsSetVerifiers =
            jwtClaimsSetVerifiersHolder.getOrDefault(tenantKey, new HashMap<>()).get(clientId);

        if (CollectionUtils.isEmpty(jwtClaimsSetVerifiers)) {
            throw new NotFoundException("Jwt claims verifiers for tenant [" + tenantKey
                + "] not found with clientId [" + clientId + "]. Check tenant idp configuration.");
        }
        return jwtClaimsSetVerifiers;
    }

    @SneakyThrows
    private Map.Entry<String, List<JwtClaimsSetVerifier>> getJwtClaimsSetVerifiers(
        IdpPublicClientConfig idpPublicClientConfig) {

        URL issuerUrl = new URL(idpPublicClientConfig.getOpenIdConfig().getIssuer());
        IssuerClaimVerifier issuerClaimVerifier = new IssuerClaimVerifier(issuerUrl);

        String clientId = idpPublicClientConfig.getClientId();
        log.debug("Build claim verifiers for client with id [{}].", clientId);
        return Map.entry(clientId, List.of(new AudienceClaimVerifier(clientId), issuerClaimVerifier));
    }
}
