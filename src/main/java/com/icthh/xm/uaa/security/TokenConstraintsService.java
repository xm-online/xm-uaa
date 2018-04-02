package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

/**
 * The {@link TokenConstraintsService} class.
 */
public class TokenConstraintsService {

    private int defaultRefreshTokenValiditySeconds = (int) TimeUnit.DAYS.toSeconds(30); // default 30 days.

    private int defaultAccessTokenValiditySeconds = (int) TimeUnit.HOURS.toSeconds(12); // default 12 hours.

    private int defaultTfaAccessTokenValiditySeconds = (int) TimeUnit.MINUTES.toSeconds(5);  // default 5 minutes.

    private boolean supportRefreshToken = false;

    private boolean reuseRefreshToken = true;

    private TenantPropertiesService tenantPropertiesService;

    private ApplicationProperties applicationProperties;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(tenantPropertiesService, "tenantPropertiesService must be set");
        Assert.notNull(applicationProperties, "applicationProperties must be set");
    }

    /**
     * The access token validity period in seconds.
     *
     * @param authentication the current authentication
     * @return the access token validity period in seconds
     */
    public int getAccessTokenValiditySeconds(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails) {
            return getAccessTokenValiditySeconds(DomainUserDetails.class.cast(principal));
        }
        return getTenantRelatedAccessTokenValiditySeconds();
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
        Integer validity;
        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails) {
            validity = DomainUserDetails.class.cast(principal).getRefreshTokenValiditySeconds();
            if (validity != null) {
                return validity;
            }
        }

        validity = tenantPropertiesService.getTenantProps().getSecurity().getRefreshTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        validity = applicationProperties.getSecurity().getRefreshTokenValiditySeconds();
        if (validity != null) {
            return validity;
        }

        return defaultRefreshTokenValiditySeconds;
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

    public boolean isReuseRefreshToken() {
        return reuseRefreshToken;
    }

    public void setTenantPropertiesService(TenantPropertiesService tenantPropertiesService) {
        this.tenantPropertiesService = tenantPropertiesService;
    }

    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Whether to support the refresh token.
     *
     * @param supportRefreshToken Whether to support the refresh token.
     */
    public void setSupportRefreshToken(boolean supportRefreshToken) {
        this.supportRefreshToken = supportRefreshToken;
    }

    /**
     * Whether to reuse refresh tokens (until expired).
     *
     * @param reuseRefreshToken Whether to reuse refresh tokens (until expired).
     */
    public void setReuseRefreshToken(boolean reuseRefreshToken) {
        this.reuseRefreshToken = reuseRefreshToken;
    }

    /**
     * The default validity (in seconds) of the refresh token. If less than or equal to zero then the tokens will be
     * non-expiring.
     *
     * @param defaultRefreshTokenValiditySeconds The validity (in seconds) of the refresh token.
     */
    public void setDefaultRefreshTokenValiditySeconds(int defaultRefreshTokenValiditySeconds) {
        this.defaultRefreshTokenValiditySeconds = defaultRefreshTokenValiditySeconds;
    }

    /**
     * The default validity (in seconds) of the access token. Zero or negative for non-expiring tokens. If a client
     * details service is set the validity period will be read from he client, defaulting to this value if not defined
     * by the client.
     *
     * @param defaultAccessTokenValiditySeconds The validity (in seconds) of the access token.
     */
    public void setDefaultAccessTokenValiditySeconds(int defaultAccessTokenValiditySeconds) {
        this.defaultAccessTokenValiditySeconds = defaultAccessTokenValiditySeconds;
    }

    public void setDefaultTfaAccessTokenValiditySeconds(int defaultTfaAccessTokenValiditySeconds) {
        this.defaultTfaAccessTokenValiditySeconds = defaultTfaAccessTokenValiditySeconds;
    }
}
