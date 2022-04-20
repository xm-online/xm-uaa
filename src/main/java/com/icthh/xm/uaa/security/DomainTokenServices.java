package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.UserDeniedAuthorizationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Overrides standard class to pass user tenant.
 */
@Slf4j
public class DomainTokenServices implements AuthorizationServerTokenServices, ResourceServerTokenServices,
    ConsumerTokenServices, InitializingBean {

    @Setter
    private TokenStore tokenStore;
    @Setter
    private TokenEnhancer tokenEnhancer;
    @Setter
    private AuthenticationManager authenticationManager;
    @Setter
    private AuthenticationRefreshProvider authenticationRefreshProvider;
    @Setter
    private TenantPropertiesService tenantPropertiesService;
    @Setter
    private TenantContextHolder tenantContextHolder;
    @Setter
    private OtpGenerator otpGenerator;
    @Setter
    private OtpSendStrategy otpSendStrategy;
    @Setter
    private OtpStore otpStore;
    @Setter
    private TokenConstraintsService tokenConstraintsService;
    @Setter
    private UserService userService;
    @Setter
    private UserSecurityValidator userSecurityValidator;

    /**
     * Initialize these token services. If no random generator is set, one will be created.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(tokenStore, "tokenStore must be set");
        Assert.notNull(otpGenerator, "otpGenerator must be set");
        Assert.notNull(otpSendStrategy, "otpSendStrategy must be set");
        Assert.notNull(otpStore, "otpStore must be set");
        Assert.notNull(userService, "userService must be set");
    }

    private boolean isTfaEnabled(OAuth2Authentication authentication) {
        if (!authentication.isAuthenticated() || !"password".equals(authentication.getOAuth2Request().getGrantType())) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof DomainUserDetails)) {
            return false;
        }

        boolean tenantTfaEnabled = tenantPropertiesService.getTenantProps().getSecurity().isTfaEnabled();
        return tenantTfaEnabled && DomainUserDetails.class.cast(principal).isTfaEnabled();
    }

    @Override
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) {
        if (isTfaEnabled(authentication)) {
            // 2FA flow
            return createTfaAccessToken(authentication);
        } else {
            // general OAuth2 flow
            return getOrCreateAccessToken(authentication);
        }
    }

    private OAuth2AccessToken getOrCreateAccessToken(OAuth2Authentication authentication) {
        OAuth2AccessToken existingAccessToken = tokenStore.getAccessToken(authentication);
        OAuth2RefreshToken refreshToken = null;
        if (existingAccessToken != null) {
            if (existingAccessToken.isExpired()) {
                if (existingAccessToken.getRefreshToken() != null) {
                    refreshToken = existingAccessToken.getRefreshToken();
                    // The token store could remove the refresh token when the
                    // access token is removed, but we want to
                    // be sure...
                    tokenStore.removeRefreshToken(refreshToken);
                }
                tokenStore.removeAccessToken(existingAccessToken);
            } else {
                // Re-store the access token in case the authentication has changed
                tokenStore.storeAccessToken(existingAccessToken, authentication);
                return existingAccessToken;
            }
        }

        // Only create a new refresh token if there wasn't an existing one
        // associated with an expired access token.
        // Clients might be holding existing refresh tokens, so we re-use it in
        // the case that the old access token
        // expired.
        if (refreshToken == null) {
            refreshToken = createRefreshToken(authentication);
        } else if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
            ExpiringOAuth2RefreshToken expiring = (ExpiringOAuth2RefreshToken) refreshToken;
            if (System.currentTimeMillis() > expiring.getExpiration().getTime()) {
                refreshToken = createRefreshToken(authentication);
            }
        }

        OAuth2AccessToken accessToken = createAccessToken(authentication, refreshToken);
        tokenStore.storeAccessToken(accessToken, authentication);
        // In case it was modified
        refreshToken = accessToken.getRefreshToken();
        if (refreshToken != null) {
            tokenStore.storeRefreshToken(refreshToken, authentication);
        }
        return accessToken;
    }

    private OAuth2AccessToken createTfaAccessToken(OAuth2Authentication authentication) {
        DefaultOAuth2AccessToken tfaToken = new DefaultOAuth2AccessToken(UUID.randomUUID().toString());

        int validitySeconds = tokenConstraintsService.getTfaAccessTokenValiditySeconds(authentication);
        if (validitySeconds > 0) {
            long timestamp = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(validitySeconds);
            tfaToken.setExpiration(new Date(timestamp));
        }
        tfaToken.setScope(authentication.getOAuth2Request().getScope());

        // generate OTP, send it, and store hashed value in UserDetails
        generateOTP(authentication);

        return (tokenEnhancer != null) ? tokenEnhancer.enhance(tfaToken, authentication) : tfaToken;
    }

    private void generateOTP(OAuth2Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!authentication.isAuthenticated() || !(principal instanceof DomainUserDetails)) {
            // should't happen but check for sure
            return;
        }

        DomainUserDetails userDetails = DomainUserDetails.class.cast(principal);

        String otp = otpGenerator.generate(authentication);
        otpStore.storeOtp(otp, authentication);
        otpSendStrategy.send(otp, userDetails);
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, InvalidGrantException.class})
    public OAuth2AccessToken refreshAccessToken(String refreshTokenValue, TokenRequest tokenRequest)
        throws AuthenticationException {

        if (!tokenConstraintsService.isSupportRefreshToken()) {
            throw new InvalidGrantException("Invalid refresh token: " + refreshTokenValue);
        }

        OAuth2RefreshToken refreshToken;
        try {
            refreshToken = tokenStore.readRefreshToken(refreshTokenValue);
        } catch (InvalidTokenException e) {
            log.warn("Could not refresh token, invalid refresh token, message: {}", e.getMessage());
            throw new InvalidTokenException("Invalid refresh token: " + refreshTokenValue, e);
        }

        if (refreshToken == null) {
            log.warn("Could not refresh token, null value given after reading, invalid refresh token");
            throw new InvalidGrantException("Invalid refresh token: " + refreshTokenValue);
        }

        OAuth2Authentication authentication = tokenStore.readAuthenticationForRefreshToken(refreshToken);

        verifyUserIsActivated(authentication);

        if (this.authenticationManager != null && !authentication.isClientOnly()) {
            // The client has already been authenticated, but the user authentication might be old now, so give it a
            // chance to re-authenticate.
            Authentication user = new PreAuthenticatedAuthenticationToken(authentication.getUserAuthentication(), "",
                                                                          authentication.getAuthorities());
            user = authenticationManager.authenticate(user);
            Object details = authentication.getDetails();
            authentication = new OAuth2Authentication(authentication.getOAuth2Request(), user);
            authentication.setDetails(details);
        }
        String clientId = authentication.getOAuth2Request().getClientId();
        if (clientId == null || !clientId.equals(tokenRequest.getClientId())) {
            throw new InvalidGrantException("Wrong client for this refresh token: " + refreshTokenValue);
        }

        // clear out any access tokens already associated with the refresh
        // token.
        tokenStore.removeAccessTokenUsingRefreshToken(refreshToken);

        if (isExpired(refreshToken)) {
            tokenStore.removeRefreshToken(refreshToken);
            throw new InvalidTokenException("Invalid refresh token (expired): " + refreshToken);
        }

        authentication = createRefreshedAuthentication(authentication, tokenRequest);

        if (authenticationRefreshProvider != null) {
            Authentication user = authenticationRefreshProvider.refresh(authentication);
            authentication = new OAuth2Authentication(authentication.getOAuth2Request(), user);
        }

        if (tenantPropertiesService.getTenantProps().getSecurity().isReIssueRefreshToken()) {
            tokenStore.removeRefreshToken(refreshToken);
            refreshToken = createRefreshToken(authentication);
        }

        OAuth2AccessToken accessToken = createAccessToken(authentication, refreshToken);
        tokenStore.storeAccessToken(accessToken, authentication);
        if (tenantPropertiesService.getTenantProps().getSecurity().isReIssueRefreshToken()) {
            tokenStore.storeRefreshToken(accessToken.getRefreshToken(), authentication);
        }
        return accessToken;
    }

    /**
     * Ensures that {@code user} by {@link Authentication#getPrincipal()} is activated
     *
     * @param authentication the authentication token
     * @throws InvalidTokenException            in case the authentication token is invalid
     * @throws UserDeniedAuthorizationException in case {@code user} not exist or not activated
     * @see User#isActivated()
     */
    private void verifyUserIsActivated(Authentication authentication) {
        if (!userSecurityValidator.isUserActivated(authentication)) {
            throw new UserNotActivatedException("User was not activated");
        }
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        return tokenStore.getAccessToken(authentication);
    }

    /**
     * Create a refreshed authentication.
     *
     * @param authentication The authentication.
     * @param request        The scope for the refreshed token.
     * @return The refreshed authentication.
     * @throws InvalidScopeException If the scope requested is invalid or wider than the original scope.
     */
    private static OAuth2Authentication createRefreshedAuthentication(OAuth2Authentication authentication,
                                                                      TokenRequest request) {
        OAuth2Authentication narrowed = authentication;
        Set<String> scope = request.getScope();
        OAuth2Request clientAuth = authentication.getOAuth2Request().refresh(request);
        if (scope != null && !scope.isEmpty()) {
            Set<String> originalScope = clientAuth.getScope();
            if (originalScope == null || !originalScope.containsAll(scope)) {
                throw new InvalidScopeException("Unable to narrow the scope of the client authentication to " + scope
                                                    + ".", originalScope);
            } else {
                clientAuth = clientAuth.narrowScope(scope);
            }
        }
        Object details = authentication.getDetails();
        narrowed = new OAuth2Authentication(clientAuth, authentication.getUserAuthentication());
        narrowed.setDetails(details);
        return narrowed;
    }

    private static boolean isExpired(OAuth2RefreshToken refreshToken) {
        if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
            ExpiringOAuth2RefreshToken expiringToken = (ExpiringOAuth2RefreshToken) refreshToken;
            return expiringToken.getExpiration() == null
                || System.currentTimeMillis() > expiringToken.getExpiration().getTime();
        }
        return false;
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        return tokenStore.readAccessToken(accessToken);
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessTokenValue) throws AuthenticationException,
        InvalidTokenException {
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(accessTokenValue);
        if (accessToken == null) {
            throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
        } else if (accessToken.isExpired()) {
            tokenStore.removeAccessToken(accessToken);
            throw new InvalidTokenException("Access token expired: " + accessTokenValue.substring(0, 200));
        } else if (accessToken.getAdditionalInformation().containsKey("tfaVerificationKey")) {
            throw new InvalidTokenException("TfaAccess token can not be used to access API");
        }

        OAuth2Authentication result = tokenStore.readAuthentication(accessToken);
        if (result == null) {
            // in case of race condition
            throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
        }

        return result;
    }

    @Override
    public boolean revokeToken(String tokenValue) {
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
        if (accessToken == null) {
            return false;
        }
        if (accessToken.getRefreshToken() != null) {
            tokenStore.removeRefreshToken(accessToken.getRefreshToken());
        }
        tokenStore.removeAccessToken(accessToken);
        return true;
    }

    private OAuth2RefreshToken createRefreshToken(OAuth2Authentication authentication) {
        if (!tokenConstraintsService.isSupportRefreshToken(authentication.getOAuth2Request())) {
            return null;
        }
        int validitySeconds = tokenConstraintsService.getRefreshTokenValiditySeconds(authentication);
        String value = UUID.randomUUID().toString();
        if (validitySeconds > 0) {
            return new DefaultExpiringOAuth2RefreshToken(value, new Date(System.currentTimeMillis()
                                                                             + (validitySeconds * 1000L)));
        }
        return new DefaultOAuth2RefreshToken(value);
    }

    private OAuth2AccessToken createAccessToken(OAuth2Authentication authentication, OAuth2RefreshToken refreshToken) {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(UUID.randomUUID().toString());
        int validitySeconds = tokenConstraintsService.getAccessTokenValiditySeconds(authentication);
        if (validitySeconds > 0) {
            token.setExpiration(new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
        }
        token.setRefreshToken(refreshToken);
        token.setScope(authentication.getOAuth2Request().getScope());

        return (tokenEnhancer != null) ? tokenEnhancer.enhance(token, authentication) : token;
    }
}
