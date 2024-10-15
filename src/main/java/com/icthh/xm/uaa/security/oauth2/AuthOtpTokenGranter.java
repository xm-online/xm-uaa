package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
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
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthOtpTokenGranter extends AbstractTokenGranter {

    private static String TOKEN_REQUEST_OTP_PARAM = "auth_otp";
    private static String TOKEN_REQUEST_ACCESS_TOKEN = "access_token";
    private static String TOKEN_ADD_INFO_LOGINS = "logins";

    private final DomainUserDetailsService domainUserDetailsService;
    private final TokenStore tokenStore;
    private final GrantedAuthoritiesMapper authoritiesMapper;

    public AuthOtpTokenGranter(DomainUserDetailsService domainUserDetailsService,
                               TokenStore tokenStore,
                               AuthorizationServerTokenServices tokenServices,
                               ClientDetailsService clientDetailsService,
                               OAuth2RequestFactory requestFactory) {
        super(tokenServices, clientDetailsService, requestFactory, GrantType.OTP.getValue());
        this.domainUserDetailsService = domainUserDetailsService;
        this.tokenStore = tokenStore;
        this.authoritiesMapper = new NullAuthoritiesMapper();
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Authentication authentication = getAuthenticationToken(tokenRequest);
        return new OAuth2Authentication(tokenRequest.createOAuth2Request(client), authentication);
    }

    private Authentication getAuthenticationToken(TokenRequest tokenRequest) {
        String otpCode = getOtpCodeFromParameters(tokenRequest, TOKEN_REQUEST_OTP_PARAM);
        String accessToken = getOtpCodeFromParameters(tokenRequest, TOKEN_REQUEST_ACCESS_TOKEN);

        OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(accessToken);

        if (oAuth2AccessToken.isExpired()) {
            throw new InvalidTokenException("OAuth2 access token expired");
        }

        String login = getLoginFromToken(oAuth2AccessToken);
        DomainUserDetails domainUserDetails = domainUserDetailsService.loadUserByUsername(login);

        if (!otpCode.equals(domainUserDetails.getAuthOtpCode())) {
            throw new InvalidGrantException("Authorization otp code is invalid");
        }
        Collection<? extends GrantedAuthority> authorities =
            authoritiesMapper.mapAuthorities(domainUserDetails.getAuthorities());

        XmAuthenticationToken userAuthenticationToken = new XmAuthenticationToken(domainUserDetails, authorities);
        userAuthenticationToken.setDetails(tokenRequest.getRequestParameters());

        return userAuthenticationToken;
    }

    private String getOtpCodeFromParameters(TokenRequest tokenRequest, String key) {
        // Remove key if present to prevent leaks
        HashMap<String, String> modifiable = new HashMap<>(tokenRequest.getRequestParameters());
        String param = modifiable.remove(key);
        tokenRequest.setRequestParameters(modifiable);

        if (StringUtils.isEmpty(param)) {
            throw new InvalidGrantException("Missing token request param: " + key);
        }
        return param;
    }

    private String getLoginFromToken(OAuth2AccessToken oAuth2AccessToken) {
        Map<String, Object> additionalInfo = oAuth2AccessToken.getAdditionalInformation();
        String loginsJson = additionalInfo.get(TOKEN_ADD_INFO_LOGINS).toString();

        if (StringUtils.isEmpty(loginsJson)) {
            throw new InvalidGrantException("User login is missing");
        }

        loginsJson = loginsJson
            .replaceAll("\\[\\{", "")
            .replaceAll("}]", "");

        return Arrays.stream(loginsJson.split("}, \\{"))
            .flatMap(e -> Arrays.stream(e.split(", ")))
            .filter(r -> r.startsWith("login="))
            .findFirst()
            .orElseThrow(() -> new InvalidGrantException("User login is missing"))
            .replaceAll("login=", "");
    }
}
