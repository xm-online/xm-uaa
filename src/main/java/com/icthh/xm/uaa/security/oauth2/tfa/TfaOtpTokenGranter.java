package com.icthh.xm.uaa.security.oauth2.tfa;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationToken.OtpCredentials;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpMsAuthenticationToken.OtpMsCredentials;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.otp.OtpType;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_ID;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY;

/**
 * The {@link TfaOtpTokenGranter} class.
 */
public class TfaOtpTokenGranter extends AbstractTokenGranter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TfaOtpTokenGranter.class);

    private static final String GRANT_TYPE = "tfa_otp_token";

    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;
    private final TokenStore tokenStore;
    private final AuthenticationManager authenticationManager;

    public TfaOtpTokenGranter(TenantContextHolder tenantContextHolder,
                              TenantPropertiesService tenantPropertiesService,
                              AuthorizationServerTokenServices tokenServices,
                              ClientDetailsService clientDetailsService,
                              OAuth2RequestFactory requestFactory,
                              TokenStore tokenStore,
                              AuthenticationManager authenticationManager) {
        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
        this.tenantPropertiesService = tenantPropertiesService;
        this.tenantContextHolder = tenantContextHolder;
        this.tokenStore = tokenStore;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        // get request parameters and remove sensitive data
        Map<String, String> parameters = new LinkedHashMap<>(tokenRequest.getRequestParameters());
        String otp = parameters.remove("otp");
        String tfaAccessTokenType = parameters.remove("tfa_access_token_type");
        String tfaAccessToken = parameters.remove("tfa_access_token");

        // prevent sensitive information to be copied to token details
        tokenRequest.setRequestParameters(parameters);

        // validate token type
        validateTokenType(tfaAccessTokenType);

        // parse TFA access token
        OAuth2AccessToken tfaOAuth2AccessToken = tokenStore.readAccessToken(tfaAccessToken);

        // validate TFA access token
        validateTfaAccessToken(tfaOAuth2AccessToken);

        String username = (String) tfaOAuth2AccessToken.getAdditionalInformation().get("user_name");

        String encodedOtp = (String) tfaOAuth2AccessToken.getAdditionalInformation().get(TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY);
        String tfaOtpId = (String) tfaOAuth2AccessToken.getAdditionalInformation().get(TOKEN_AUTH_DETAILS_TFA_OTP_ID);
        Long otpId = tfaOtpId != null ? Long.valueOf(tfaOtpId) : null;

        Authentication userAuthentication = createTfaAuthentication(parameters, otp, username, encodedOtp, otpId);

        Authentication resultUserAuth;
        try {
            resultUserAuth = authenticationManager.authenticate(userAuthentication);
        } catch (AccountStatusException | BadCredentialsException e) {
            // AccountStatusException: covers expired, locked, disabled cases (mentioned in section 5.2, draft 31)
            // BadCredentialsException: If the username/password are wrong the spec says we should send 400/invalid grant
            logger.error("authentication error: {}", e);
            throw new InvalidGrantException(e.getMessage());
        }

        if (resultUserAuth == null || !resultUserAuth.isAuthenticated()) {
            throw new InvalidGrantException("Could not authenticate user: " + username);
        }

        return new OAuth2Authentication(tokenRequest.createOAuth2Request(client), resultUserAuth);
    }

    private Authentication createTfaAuthentication(Map<String, String> parameters, String otp, String username, String encodedOtp, Long otpId) {
        OtpType tfaOtpType = tenantPropertiesService.getTenantProps().getSecurity().getTfaOtpType();

        if (OtpType.EMBEDDED.equals(tfaOtpType)) {
            TfaOtpAuthenticationToken tfaOtpAuthenticationToken = new TfaOtpAuthenticationToken(username, new OtpCredentials(otp, encodedOtp));
            tfaOtpAuthenticationToken.setDetails(parameters);
            return tfaOtpAuthenticationToken;
        } else if (OtpType.OTP_MS.equals(tfaOtpType)) {
            TfaOtpMsAuthenticationToken tfaOtpMsAuthenticationToken = new TfaOtpMsAuthenticationToken(username, new OtpMsCredentials(otp, otpId));
            tfaOtpMsAuthenticationToken.setDetails(parameters);
            return tfaOtpMsAuthenticationToken;
        } else {
            throw new NotImplementedException("Not implemented tfaOtpType: " + tfaOtpType);
        }
    }

    private void validateTokenType(String tfaAccessTokenType) {
        if (StringUtils.isBlank(tfaAccessTokenType)) {
            throw new InvalidTokenException("TFA access token type is required");
        }

        if (!"bearer".equalsIgnoreCase(tfaAccessTokenType)) {
            throw new InvalidTokenException("TFA access token type " + tfaAccessTokenType + " not supported");
        }
    }

    @Override
    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest) {
        return getTokenServices().createAccessToken(getOAuth2Authentication(client, tokenRequest));
    }

    private void validateTfaAccessToken(OAuth2AccessToken tfaOAuth2AccessToken) {
        // check is token expired
        if (tfaOAuth2AccessToken.isExpired()) {
            throw new InvalidTokenException("TFA access token expired");
        }

        Map<String, Object> additionalInfo = tfaOAuth2AccessToken.getAdditionalInformation();

        // is tenant exist & coincides with current tenant
        Object tenantKeyObj = additionalInfo.get(Constants.AUTH_TENANT_KEY);
        if (tenantKeyObj == null || !(tenantKeyObj instanceof String)) {
            LOGGER.debug("TFA access token has no tenant or tenant value has invalid type");
            throw new InvalidGrantException("Bad credentials");
        }
        String tenantKey = String.valueOf(tenantKeyObj);

        String currentTenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        if (!Objects.equals(tenantKey, currentTenantKey)) {
            LOGGER.debug("TFA access token granted to other tenant");
            throw new InvalidGrantException("Bad credentials");
        }

        // is user name exist
        Object userName = additionalInfo.get("user_name");
        if (userName == null || StringUtils.isBlank(String.valueOf(userName))) {
            LOGGER.debug("TFA access token has no username");
            throw new InvalidGrantException("Bad credentials");
        }

        // is encoded OTP exist with embedded tfa flow
        Object encodedOtp = additionalInfo.get(TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY);
        // is otpId exist with flow integration otp microservice
        Object otpId = additionalInfo.get(TOKEN_AUTH_DETAILS_TFA_OTP_ID);
        if ((encodedOtp == null || StringUtils.isBlank(String.valueOf(encodedOtp))) &&
            (otpId == null || StringUtils.isBlank(String.valueOf(otpId)))) {
            LOGGER.debug("TFA access token has no OTP");
            throw new InvalidGrantException("Bad credentials");
        }
    }

}
