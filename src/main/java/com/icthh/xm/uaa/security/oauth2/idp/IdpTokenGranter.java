package com.icthh.xm.uaa.security.oauth2.idp;

import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmAuthenticationToken;
import com.icthh.xm.uaa.service.IdpIdTokenMappingService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.uaa.security.DomainUserDetailsService.buildDomainUserDetails;

/**
 * This class responsible for handling requests with idp token
 */
@Slf4j
@LepService(group = "security.idp")
public class IdpTokenGranter extends AbstractTokenGranter {

    public static final String GRANT_TYPE_IDP_TOKEN = "idp_token";

    private final XmJwkTokenStore jwkTokenStore;
    private final DomainUserDetailsService domainUserDetailsService;
    private final UserService userService;
    private final UserLoginService userLoginService;
    private final IdpIdTokenMappingService idpIdTokenMappingService;
    private final TenantContextHolder tenantContextHolder;

    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public IdpTokenGranter(AuthorizationServerTokenServices tokenServices,
                           ClientDetailsService clientDetailsService,
                           @Lazy OAuth2RequestFactory requestFactory,
                           XmJwkTokenStore jwkTokenStore,
                           DomainUserDetailsService domainUserDetailsService,
                           UserService userService,
                           UserLoginService userLoginService,
                           IdpIdTokenMappingService idpIdTokenMappingService,
                           TenantContextHolder tenantContextHolder) {
        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE_IDP_TOKEN);
        this.jwkTokenStore = jwkTokenStore;
        this.domainUserDetailsService = domainUserDetailsService;
        this.userService = userService;
        this.userLoginService = userLoginService;
        this.idpIdTokenMappingService = idpIdTokenMappingService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest) {
        return getTokenServices().createAccessToken(getOAuth2Authentication(client, tokenRequest));
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Map<String, String> parameters = new LinkedHashMap<>(tokenRequest.getRequestParameters());
        Authentication authentication = getOAuth2AuthenticationFromToken(parameters);

        return new OAuth2Authentication(tokenRequest.createOAuth2Request(client), authentication);
    }

    @SneakyThrows
    private Authentication getOAuth2AuthenticationFromToken(Map<String, String> parameters) {
        return getUserAuthenticationToken(parameters);
    }

    private XmAuthenticationToken getUserAuthenticationToken(Map<String, String> parameters) {
        String idpToken = parameters.get("token");

        // parse IDP id token
        OAuth2AccessToken idpOAuth2IdToken = jwkTokenStore.readAccessToken(idpToken);

        idpIdTokenMappingService.validateIdpIdToken(idpOAuth2IdToken);

        //user + role section
        DomainUserDetails userDetails = retrieveDomainUserDetails(idpOAuth2IdToken);
        Collection<? extends GrantedAuthority> authorities =
            authoritiesMapper.mapAuthorities(userDetails.getAuthorities());

        //build container for user
        XmAuthenticationToken userAuthenticationToken = new XmAuthenticationToken(userDetails, authorities);
        userAuthenticationToken.setDetails(parameters);

        return userAuthenticationToken;
    }

    private DomainUserDetails retrieveDomainUserDetails(OAuth2AccessToken idpOAuth2IdToken) {
        String userIdentity = idpIdTokenMappingService.mapIdpIdTokenToIdentity(idpOAuth2IdToken);
        Optional<DomainUserDetails> userDetails = domainUserDetailsService.retrieveUserByUsername(userIdentity);

        if (userDetails.isEmpty()) {
            log.info("User not found by identity: {}, new user will be created", userIdentity);
            User newUser = createUser(userIdentity, idpOAuth2IdToken);
            userDetails = Optional.of(buildDomainUserDetails(userIdentity, tenantContextHolder.getTenantKey(), newUser));
        }
        log.info("Mapped user for identity:{} is {}", userIdentity, userDetails);
        return userDetails.get();
    }

    private User createUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        UserDTO userDTO = idpIdTokenMappingService.mapIdpIdTokenToXmUser(userIdentity, idpOAuth2IdToken);

        userLoginService.normalizeLogins(userDTO.getLogins());
        userLoginService.verifyLoginsNotExist(userDTO.getLogins());

        userDTO.setRoleKey(idpIdTokenMappingService.mapIdpIdTokenToRole(userIdentity, idpOAuth2IdToken));

        return userService.createUser(userDTO);
    }
}
