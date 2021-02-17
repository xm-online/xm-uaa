package com.icthh.xm.uaa.security.oauth2.idp;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.IdpAuthenticationToken;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationServiceException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TODO add java doc and test's, maybe with lep's?
 */
@Slf4j
@LepService(group = "security.idp")
public class IdpTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "idp_token";

    private static final String FIRST_NAME_CONFIG_ATTRIBUTE = "firstNameAttribute";
    private static final String LAST_NAME_CONFIG_ATTRIBUTE = "lastNameAttribute";
    private static final String USER_IDENTITY_CONFIG_TYPE = "userIdentityType";
    private static final String USER_IDENTITY_CONFIG_ATTRIBUTE = "userIdentityAttribute";

    private final XmJwkTokenStore jwkTokenStore;
    private final DomainUserDetailsService domainUserDetailsService;
    private final TenantPropertiesService tenantPropertiesService;
    private final UserService userService;
    private final UserLoginService userLoginService;
    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public IdpTokenGranter(AuthorizationServerTokenServices tokenServices,
                           ClientDetailsService clientDetailsService,
                           @Lazy OAuth2RequestFactory requestFactory,
                           XmJwkTokenStore jwkTokenStore,
                           DomainUserDetailsService domainUserDetailsService,
                           TenantPropertiesService tenantPropertiesService,
                           UserService userService,
                           UserLoginService userLoginService) {
        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
        this.jwkTokenStore = jwkTokenStore;
        this.domainUserDetailsService = domainUserDetailsService;
        this.tenantPropertiesService = tenantPropertiesService;
        this.userService = userService;
        this.userLoginService = userLoginService;
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

    private IdpAuthenticationToken getUserAuthenticationToken(Map<String, String> parameters) {
        String idpToken = parameters.get("token");

        // parse IDP id token
        OAuth2AccessToken idpOAuth2IdToken = jwkTokenStore.readAccessToken(idpToken);

        validateIdpIdToken(idpOAuth2IdToken);

        //user + role section
        DomainUserDetails userDetails = retrieveDomainUserDetails(idpOAuth2IdToken);
        Collection<? extends GrantedAuthority> authorities =
            authoritiesMapper.mapAuthorities(userDetails.getAuthorities());

        //build container for user
        IdpAuthenticationToken userAuthenticationToken = new IdpAuthenticationToken(userDetails, authorities);
        userAuthenticationToken.setDetails(parameters);

        return userAuthenticationToken;
    }

    private DomainUserDetails retrieveDomainUserDetails(OAuth2AccessToken idpOAuth2IdToken) {
        String userIdentity = mapIdpIdTokenToIdentity(idpOAuth2IdToken);
        Optional<DomainUserDetails> userDetails = domainUserDetailsService.retrieveUserByUsername(userIdentity);

        if (userDetails.isEmpty()) {
            log.info("User not found by identity: {}, new user will be created", userIdentity);
            User newUser = createUser(userIdentity, idpOAuth2IdToken);
            userDetails = Optional.of(DomainUserDetailsService.buildDomainUserDetails(userIdentity, getTenantKey(), newUser));
        }
        log.info("Mapped user for identity:{} is {}", userIdentity, userDetails);
        return userDetails.get();
    }

    @LogicExtensionPoint(value = "MapIdpIdTokenToIdentity")
    public String mapIdpIdTokenToIdentity(OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        Map<String, String> defaultClaimsMapping = getDefaultClaimsMapping();

        String userIdentityAttribute = defaultClaimsMapping.get(USER_IDENTITY_CONFIG_ATTRIBUTE);

        return (String) additionalInformation.get(userIdentityAttribute);
    }

    private User createUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        UserDTO userDTO = mapIdpIdTokenToXmUser(userIdentity, idpOAuth2IdToken);

        userLoginService.normalizeLogins(userDTO.getLogins());
        userLoginService.verifyLoginsNotExist(userDTO.getLogins());

        userDTO.setRoleKey(mapIdpIdTokenToRole(userIdentity, idpOAuth2IdToken));

        return userService.createUser(userDTO);
    }

    @LogicExtensionPoint(value = "ValidateIdpIdToken")
    public void validateIdpIdToken(OAuth2AccessToken idpOAuth2IdToken) {
        //validate any additional claims viaLEP
    }

    @LogicExtensionPoint(value = "MapIdpIdTokenToXmUser")
    public UserDTO mapIdpIdTokenToXmUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        UserDTO userDTO = new UserDTO();

        //base info mapping
        Map<String, String> defaultClaimsMapping = getDefaultClaimsMapping();
        String userFirstNameAttribute = defaultClaimsMapping.get(FIRST_NAME_CONFIG_ATTRIBUTE);

        String userLastNameAttribute = defaultClaimsMapping.get(LAST_NAME_CONFIG_ATTRIBUTE);

        userDTO.setFirstName((String) additionalInformation.get(userFirstNameAttribute));
        userDTO.setLastName((String) additionalInformation.get(userLastNameAttribute));
        //login mapping

        String userIdentityType = UserLoginType
            .fromString(defaultClaimsMapping.get(USER_IDENTITY_CONFIG_TYPE))
            .getValue();

        UserLogin emailUserLogin = new UserLogin();
        emailUserLogin.setLogin(userIdentity);
        emailUserLogin.setTypeKey(userIdentityType);

        userDTO.setLogins(List.of(emailUserLogin));
        return userDTO;
    }

    @SneakyThrows
    @LogicExtensionPoint(value = "MapIdpIdTokenToRole")
    public String mapIdpIdTokenToRole(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        TenantProperties tenantProps = tenantPropertiesService.getTenantProps();
        TenantProperties.Security security = tenantProps.getSecurity();

        if (security == null) {
            log.debug("Default role for tenant [{}] not specified.", getTenantKey());
            throw new AuthenticationServiceException("Authentication failed " +
                "cause of tenant configuration lack [" + getTenantKey() + "].");
        }
        return security.getDefaultUserRole();
    }

    private String getTenantKey() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantPropertiesService.getTenantContextHolder());
    }

    private Map<String, String> getDefaultClaimsMapping() {
        TenantProperties tenantProps = tenantPropertiesService.getTenantProps();
        TenantProperties.Security security = tenantProps.getSecurity();

        TenantProperties.Security.Idp idp = security.getIdp();

        if (idp == null || idp.getDefaultIdpClaimMapping() == null
            || idp.getDefaultIdpClaimMapping().getFirstNameAttribute() == null
            || idp.getDefaultIdpClaimMapping().getLastNameAttribute() == null
            || idp.getDefaultIdpClaimMapping().getUserIdentityAttribute() == null
            || idp.getDefaultIdpClaimMapping().getUserIdentityType() == null
        ) {
            log.debug("User Identity mapping not fully specified in tenant [{}] configuration.", getTenantKey());
            throw new AuthenticationServiceException("Authentication failed " +
                "cause of tenant configuration lack [" + getTenantKey() + "].");
        }
        TenantProperties.Security.Idp.DefaultIdpClaimMapping defaultIdpClaimMapping = idp.getDefaultIdpClaimMapping();

        return Map.of(
            FIRST_NAME_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getFirstNameAttribute(),
            LAST_NAME_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getLastNameAttribute(),
            USER_IDENTITY_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getUserIdentityAttribute(),
            USER_IDENTITY_CONFIG_TYPE, defaultIdpClaimMapping.getUserIdentityType()
        );
    }

}
