package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmAuthenticationToken;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
public class AuthOtpTokenGranter extends AbstractTokenGranter {

    private static String TOKEN_REQUEST_OTP_PARAM = "auth_otp";
    private static String TOKEN_REQUEST_USERNAME_PARAM = "username";

    private final DomainUserDetailsService domainUserDetailsService;
    private final GrantedAuthoritiesMapper authoritiesMapper;
    private final TenantPropertiesService tenantPropertiesService;

    public AuthOtpTokenGranter(DomainUserDetailsService domainUserDetailsService,
                               AuthorizationServerTokenServices tokenServices,
                               ClientDetailsService clientDetailsService,
                               OAuth2RequestFactory requestFactory,
                               TenantPropertiesService tenantPropertiesService) {
        super(tokenServices, clientDetailsService, requestFactory, GrantType.OTP.getValue());
        this.domainUserDetailsService = domainUserDetailsService;
        this.tenantPropertiesService = tenantPropertiesService;
        this.authoritiesMapper = new NullAuthoritiesMapper();
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Authentication authentication = getAuthenticationToken(tokenRequest);
        return new OAuth2Authentication(tokenRequest.createOAuth2Request(client), authentication);
    }

    private Authentication getAuthenticationToken(TokenRequest tokenRequest) {
        String otpCode = getParamFromTokenRequest(tokenRequest, TOKEN_REQUEST_OTP_PARAM);
        String login = prepareLoginValue(getParamFromTokenRequest(tokenRequest, TOKEN_REQUEST_USERNAME_PARAM));

        DomainUserDetails domainUserDetails = domainUserDetailsService.loadUserByUsername(login);

        if (!otpCode.equals(domainUserDetails.getAuthOtpCode())
            || isExpiredOtp(domainUserDetails.getAuthOtpCodeCreationDate())) {
            throw new InvalidGrantException("Authorization otp code is invalid");
        }
        Collection<? extends GrantedAuthority> authorities =
            authoritiesMapper.mapAuthorities(domainUserDetails.getAuthorities());

        XmAuthenticationToken userAuthenticationToken = new XmAuthenticationToken(domainUserDetails, authorities);
        userAuthenticationToken.setDetails(tokenRequest.getRequestParameters());

        return userAuthenticationToken;
    }

    private boolean isExpiredOtp(Instant authOtpCodeCreationDate) {
        if (authOtpCodeCreationDate == null) {
            return false;
        }
        Duration actualInterval = Duration.between(authOtpCodeCreationDate, Instant.now());
        int allowedInterval = getAllowedInterval(tenantPropertiesService.getTenantProps());

        return allowedInterval < 0 || actualInterval.getSeconds() >= allowedInterval;
    }

    private static int getAllowedInterval(TenantProperties tenantProps) {
        return Optional.ofNullable(tenantProps)
            .map(TenantProperties::getSecurity)
            .map(TenantProperties.Security::getOtpThrottlingLifeTimeInSeconds)
            .orElse(60);
    }

    private String prepareLoginValue(String login) {
        if (UserLoginType.MSISDN.equals(UserLoginType.getByRegex(login)) && !login.startsWith("+")) {
            return "+" + login;
        }
        return login;
    }

    private String getParamFromTokenRequest(TokenRequest tokenRequest, String key) {
        // Remove key if present to prevent leaks
        HashMap<String, String> modifiable = new HashMap<>(tokenRequest.getRequestParameters());
        String param = modifiable.remove(key);
        tokenRequest.setRequestParameters(modifiable);

        if (StringUtils.isEmpty(param)) {
            throw new InvalidGrantException("Missing token request param: " + key);
        }
        return param;
    }
}
