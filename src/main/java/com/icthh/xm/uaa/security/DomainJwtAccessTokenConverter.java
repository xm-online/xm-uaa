package com.icthh.xm.uaa.security;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.uaa.config.Constants.AUTH_ADDITIONAL_DETAILS;
import static com.icthh.xm.uaa.config.Constants.AUTH_LOGINS_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_ROLE_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;
import static com.icthh.xm.uaa.config.Constants.CREATE_TOKEN_TIME;
import static com.icthh.xm.uaa.config.Constants.MULTI_ROLE_ENABLED;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_OTP_CHANNEL_TYPE;
import static com.icthh.xm.uaa.config.Constants.TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY;
import static org.apache.commons.collections.MapUtils.isNotEmpty;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

/**
 * Overrides to add and get token tenant.
 */
@RequiredArgsConstructor
public class DomainJwtAccessTokenConverter extends JwtAccessTokenConverter {

    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;
    private final DomainJwtAccessTokenDetailsPostProcessor tokenDetailsProcessor;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (accessToken instanceof DefaultOAuth2AccessToken) {
            @SuppressWarnings("unchecked")
            Map<String, Object> authDetails = (Map<String, Object>) authentication.getDetails();
            if (authDetails == null) {
                authDetails = new HashMap<>();
                authentication.setDetails(authDetails);
            }
            enrichCustomDetails(authentication, authDetails);
            DefaultOAuth2AccessToken.class.cast(accessToken).setAdditionalInformation(authDetails);
        }
        return super.enhance(accessToken, authentication);
    }

    private void enrichCustomDetails(OAuth2Authentication authentication, Map<String, Object> details) {
        final Object principal = authentication.getPrincipal();
        details.put(AUTH_TENANT_KEY, getRequiredTenantKeyValue(tenantContextHolder));

        tokenDetailsProcessor.processJwtAccessTokenDetails(authentication, details);

        if (principal instanceof DomainUserDetails) {
            final DomainUserDetails userDetails = (DomainUserDetails) principal;

            details.put(AUTH_TENANT_KEY, userDetails.getTenant());
            details.put(AUTH_USER_KEY, userDetails.getUserKey());
            details.put(CREATE_TOKEN_TIME, System.currentTimeMillis());

            if (userDetails.isTfaApplied()) {
                String tfaHashedOtp = userDetails.getTfaEncodedOtp().orElse(null);
                details.put(TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY, tfaHashedOtp);

                String tfaOtpChannelTypeName = userDetails.getTfaOtpChannelType().map(OtpChannelType::getTypeName).orElse(null);
                details.put(TOKEN_AUTH_DETAILS_TFA_OTP_CHANNEL_TYPE, tfaOtpChannelTypeName);
            } else {
                details.put(AUTH_LOGINS_KEY, userDetails.getLogins());
                details.put(AUTH_ROLE_KEY, getOptionalRoleKey(userDetails.getAuthorities()));
                Map<String, Object> additionalDetails =
                    (Map<String, Object>) details.getOrDefault(AUTH_ADDITIONAL_DETAILS, new HashMap<>());
                if (isNotEmpty(userDetails.getAdditionalDetails())) {
                    additionalDetails.putAll(userDetails.getAdditionalDetails());
                }
                if(tenantPropertiesService.getTenantProps().getSecurity().isMultiRoleEnabled()){
                    additionalDetails.put(MULTI_ROLE_ENABLED, true);
                }
                if(additionalDetails.size() > 0){
                    details.put(AUTH_ADDITIONAL_DETAILS, additionalDetails);
                }
            }
        }
    }

    private static String getOptionalRoleKey(Collection<GrantedAuthority> authorities) {
        if (CollectionUtils.isNotEmpty(authorities)) {
            GrantedAuthority authority = authorities.iterator().next();
            return authority != null ? authority.getAuthority() : null;
        }
        return null;
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> tokenMap) {
        final Map<String, Object> details = new HashMap<>();

        remap(tokenMap, details, AUTH_TENANT_KEY);
        remap(tokenMap, details, AUTH_USER_KEY);

        remap(tokenMap, details, CREATE_TOKEN_TIME);
        remap(tokenMap, details, AUTH_ADDITIONAL_DETAILS);
        remap(tokenMap, details, AUTH_ROLE_KEY);
        remap(tokenMap, details, TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY);
        remap(tokenMap, details, TOKEN_AUTH_DETAILS_TFA_OTP_CHANNEL_TYPE);
        remap(tokenMap, details, AUTH_LOGINS_KEY);

        final OAuth2Authentication authentication = super.extractAuthentication(tokenMap);
        authentication.setDetails(details);
        return authentication;
    }

    private static void remap(Map<String, ?> src, Map<String, Object> dst, String key) {
        if (src.containsKey(key)) {
            dst.put(key, src.get(key));
        }
    }

}
