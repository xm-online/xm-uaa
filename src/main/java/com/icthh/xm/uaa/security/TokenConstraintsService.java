package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * The {@link TokenConstraintsService} class.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@IgnoreLogginAspect
public class TokenConstraintsService {

    private int defaultRefreshTokenValiditySeconds = (int) TimeUnit.DAYS.toSeconds(30); // default 30 days.

    private int defaultAccessTokenValiditySeconds = (int) TimeUnit.HOURS.toSeconds(12); // default 12 hours.

    private int defaultTfaAccessTokenValiditySeconds = (int) TimeUnit.MINUTES.toSeconds(5);  // default 5 minutes.

    private boolean supportRefreshToken = true;

    private final TenantPropertiesService tenantPropertiesService;

    private final ApplicationProperties applicationProperties;

    private final ClientDetailsService clientDetailsService;

    /**
     * The access token validity period in seconds.
     *
     * @param authentication the current authentication
     * @return the access token validity period in seconds
     */
    public int getAccessTokenValiditySeconds(OAuth2Authentication authentication) {
        String clientId = authentication.getOAuth2Request().getClientId();
        Optional<Integer> clientSpecific = resolveClientSpecificAccessTokenValidity(clientId);
        if (clientSpecific.isPresent()) {
            log.debug("Using client-specific access token validity for clientId='{}': {} seconds", clientId, clientSpecific.get());
            return clientSpecific.get();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails) {
            return getAccessTokenValiditySeconds(DomainUserDetails.class.cast(principal));
        }

        return loadClientInfo(authentication, ClientDetails::getAccessTokenValiditySeconds)
            .orElse(getTenantRelatedAccessTokenValiditySeconds());
    }

    private <T> Optional<T> loadClientInfo(OAuth2Authentication authentication, Function<ClientDetails, T> consumer) {
        String clientId = authentication.getOAuth2Request().getClientId();
        return ofNullable(clientId)
            .map(clientDetailsService::loadClientByClientId)
            .map(consumer);
    }

    public int getAccessTokenValiditySeconds(DomainUserDetails userDetails) {
        return getAccessTokenValiditySeconds(userDetails.getAccessTokenValiditySeconds());
    }

    public int getAccessTokenValiditySeconds(Integer userAccessTokenValiditySeconds) {
        return (userAccessTokenValiditySeconds != null)
            ? userAccessTokenValiditySeconds : getTenantRelatedAccessTokenValiditySeconds();
    }

    public int getTenantRelatedAccessTokenValiditySeconds() {
        Integer validity;

        validity = tenantPropertiesService.getTenantProps().getSecurity().getAccessTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        validity = applicationProperties.getSecurity().getAccessTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        return defaultAccessTokenValiditySeconds;
    }

    /**
     * The refresh token validity period in seconds.
     *
     * @param authentication the current authentication
     * @return the refresh token validity period in seconds
     */
    public int getRefreshTokenValiditySeconds(OAuth2Authentication authentication) {
        String clientId = authentication.getOAuth2Request().getClientId();
        Optional<Integer> clientSpecific = resolveClientSpecificRefreshTokenValidity(clientId);
        if (clientSpecific.isPresent()) {
            log.debug("Using client-specific refresh token validity for clientId='{}': {} seconds",
                clientId, clientSpecific.get());
            return clientSpecific.get();
        }

        Integer validity;
        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails) {
            validity = DomainUserDetails.class.cast(principal).getRefreshTokenValiditySeconds();
            if (validity != null) {
                return validity;
            }
        }

        return loadClientInfo(authentication, ClientDetails::getRefreshTokenValiditySeconds)
            .orElse(firstNonNull(
                tenantPropertiesService.getTenantProps().getSecurity().getRefreshTokenValiditySeconds(),
                applicationProperties.getSecurity().getRefreshTokenValiditySeconds(),
                defaultRefreshTokenValiditySeconds
            )
        );
    }

    /**
     * The TFA access token validity period in seconds.
     *
     * @param authentication the current authentication
     * @return the TFA access token validity period in seconds
     */
    public int getTfaAccessTokenValiditySeconds(OAuth2Authentication authentication) {
        Integer validity;

        // trying get from User settings
        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails) {
            validity = DomainUserDetails.class.cast(principal).getTfaAccessTokenValiditySeconds();
            if (validity != null) {
                return validity;
            }
        }

        // trying to get from Tenant settings
        validity = tenantPropertiesService.getTenantProps().getSecurity().getTfaAccessTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        // trying to get from Application settings
        validity = applicationProperties.getSecurity().getTfaAccessTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        // get default
        return defaultTfaAccessTokenValiditySeconds;
    }

    /**
     * Resolves per-client access token validity from tenant properties (external {@code uaa.yaml}).
     * Returns empty if: no config exists for the client, the configured value is invalid (≤ 0),
     * or the value would exceed the global default when extension is not allowed.
     *
     * @param clientId OAuth2 client identifier
     * @return resolved access token validity seconds, or empty to fall back to default chain
     */
    Optional<Integer> resolveClientSpecificAccessTokenValidity(String clientId) {
        return resolveClientSpecificValidity(
            clientId,
            TenantProperties.Security.ClientTokenLifetime::getAccessTokenValiditySeconds,
            this::getTenantRelatedAccessTokenValiditySeconds,
            "access"
        );
    }

    /**
     * Resolves per-client refresh token validity from tenant properties (external {@code uaa.yaml}).
     * Returns empty if: no config exists for the client, the configured value is invalid (≤ 0),
     * or the value would exceed the global default when extension is not allowed.
     *
     * @param clientId OAuth2 client identifier
     * @return resolved refresh token validity seconds, or empty to fall back to default chain
     */
    Optional<Integer> resolveClientSpecificRefreshTokenValidity(String clientId) {
        return resolveClientSpecificValidity(
            clientId,
            TenantProperties.Security.ClientTokenLifetime::getRefreshTokenValiditySeconds,
            this::getTenantRelatedRefreshTokenValiditySeconds,
            "refresh"
        );
    }

    private Optional<Integer> resolveClientSpecificValidity(
        String clientId,
        Function<TenantProperties.Security.ClientTokenLifetime, Integer> extractor,
        java.util.function.IntSupplier globalDefaultSupplier,
        String tokenType
    ) {
        if (clientId == null) {
            return Optional.empty();
        }

        TenantProperties.Security security = tenantPropertiesService.getTenantProps().getSecurity();
        Map<String, TenantProperties.Security.ClientTokenLifetime> clientLifetimes = security.getClientTokenLifetimes();
        if (clientLifetimes == null || !clientLifetimes.containsKey(clientId)) {
            return Optional.empty();
        }

        TenantProperties.Security.ClientTokenLifetime lifetime = clientLifetimes.get(clientId);
        Integer configured = extractor.apply(lifetime);
        if (configured == null) {
            return Optional.empty();
        }

        if (configured <= 0) {
            log.warn("Invalid {} token validity seconds {} configured for clientId='{}'; must be > 0. " +
                    "Falling back to default.",
                tokenType, configured, clientId);
            return Optional.empty();
        }

        if (!security.isAllowClientTokenLifetimeExtension()) {
            int globalDefault = globalDefaultSupplier.getAsInt();
            if (configured > globalDefault) {
                log.warn("Client-specific {} token validity {} for clientId='{}' exceeds global default {}. "
                        + "Capping at global default. Set 'allowClientTokenLifetimeExtension: true' to " +
                        "permit extensions.",
                    tokenType, configured, clientId, globalDefault);
                return Optional.of(globalDefault);
            }
        }

        return Optional.of(configured);
    }

    private int getTenantRelatedRefreshTokenValiditySeconds() {
        return firstNonNull(
            tenantPropertiesService.getTenantProps().getSecurity().getRefreshTokenValiditySeconds(),
            applicationProperties.getSecurity().getRefreshTokenValiditySeconds(),
            defaultRefreshTokenValiditySeconds
        );
    }

    public int getDefaultRefreshTokenValiditySeconds() {
        return defaultRefreshTokenValiditySeconds;
    }

    public int getDefaultAccessTokenValiditySeconds() {
        return defaultAccessTokenValiditySeconds;
    }

    public int getDefaultTfaAccessTokenValiditySeconds() {
        return defaultTfaAccessTokenValiditySeconds;
    }

    public boolean isSupportRefreshToken() {
        return supportRefreshToken;
    }

    /**
     * Is a refresh token supported for this client.
     *
     * @param clientAuth the current authorization request
     * @return boolean to indicate if refresh token is supported
     */
    public boolean isSupportRefreshToken(OAuth2Request clientAuth) {
        return isSupportRefreshToken();
    }
}
