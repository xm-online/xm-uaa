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
import com.icthh.xm.uaa.security.TenantNotProvidedException;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.IdpAuthenticationToken;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.NoSuchElementException;

@Slf4j
@LepService(group = "security.idp")
public class IdpTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "idp_token";

    private final CustomJwkTokenStore jwkTokenStore;
    private final DomainUserDetailsService domainUserDetailsService;
    private final TenantPropertiesService tenantPropertiesService;
    private final UserService userService;
    private final UserLoginService userLoginService;
    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();


    public IdpTokenGranter(AuthorizationServerTokenServices tokenServices,
                           ClientDetailsService clientDetailsService,
                           OAuth2RequestFactory requestFactory,
                           CustomJwkTokenStore jwkTokenStore,
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
        buildClaimsValidators();
        String idpToken = parameters.remove("token"); //TODO why we remove this from parameter?
        //TODO Also put all key, hardcoded names, etc to constant class (if more than one time it used) or to private static final variable

        // parse IDP id token
        OAuth2AccessToken idpOAuth2IdToken = jwkTokenStore.readAccessToken(idpToken);

        validateIdpAccessToken(idpOAuth2IdToken);

        //user + role section
        //TODO think about LEP + config
        DomainUserDetails userDetails = retrieveDomainUserDetails(idpOAuth2IdToken);
        Collection<? extends GrantedAuthority> authorities =
            authoritiesMapper.mapAuthorities(userDetails.getAuthorities());

        //build container for user
        IdpAuthenticationToken userAuthenticationToken = new IdpAuthenticationToken(userDetails, authorities);
        userAuthenticationToken.setDetails(parameters);

        return userAuthenticationToken;
    }


    //TODO do we really need this method, can we use only validateAccessToken
    private void buildClaimsValidators() {
//        Map<String, Set<JwtClaimsSetVerifier>> claimsSetVerifiers = jwkTokenStore.getJwtTokenEnhancer().getJwtClaimsSetVerifiers();
//
//        Set<JwtClaimsSetVerifier> jwtClaimsSetVerifiers = Set.of(IssuerClaimVerifier);
//        claimsSetVerifiers.put();
    }

    private DomainUserDetails retrieveDomainUserDetails(OAuth2AccessToken idpOAuth2IdToken) {
        String userIdentity = extractUserIdentity(idpOAuth2IdToken);
        DomainUserDetails userDetails = domainUserDetailsService.retrieveUserByUsername(userIdentity);

        if (userDetails == null) {
            log.info("User not found by identity: {}, new user will be created", userIdentity);
            userDetails = buildDomainUserDetails(userIdentity, idpOAuth2IdToken);
        }
        log.info("Mapped user for identity:{} is {}", userIdentity, userDetails);
        return userDetails;
    }

    @LogicExtensionPoint(value = "ExtractUserIdentity")
    public String extractUserIdentity(OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        //TODO "email" should be taken from uaa.yml: security.idp.defaultLoginAttribute whith default
        // value: email if configuration not specified
        return (String) additionalInformation.get("email");
    }

    private DomainUserDetails buildDomainUserDetails(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        log.debug("User with login: {} not exists. Creating new user.", userIdentity);

        User newUser = createUser(userIdentity, idpOAuth2IdToken);

        //TODO think how to avoid this check. What we will do if login is not email
        //TODO what the difference between userIdentity and userLogin
        //TODO userLogin used only for getUser (buildDomainUserDetails ->   User user = userLogin.getUser();)
        UserLogin userLogin = newUser.getLogins()
            .stream()
            .filter(login -> UserLoginType.EMAIL.getValue().equals(login.getTypeKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("UserLogin with type " +
                UserLoginType.EMAIL.getValue() + "not found"));

        return DomainUserDetailsService.buildDomainUserDetails(userIdentity, getTenantKey(), userLogin);
    }

    private User createUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        UserDTO userDTO = convertIdpToXmUser(userIdentity, idpOAuth2IdToken);

        userLoginService.normalizeLogins(userDTO.getLogins());
        userLoginService.verifyLoginsNotExist(userDTO.getLogins());

        userDTO.setRoleKey(mapIdpRole(userIdentity, idpOAuth2IdToken));

        return userService.createUser(userDTO);
    }

    //TODO add claim validation: audience and issuer
    @LogicExtensionPoint(value = "ValidateIdpAccessToken")
    public void validateIdpAccessToken(OAuth2AccessToken idpOAuth2IdToken) {
        //validate issuer and audience, etc, + LEP
    }


    @LogicExtensionPoint(value = "ConvertIdpToXmUser")
    public UserDTO convertIdpToXmUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        UserDTO userDTO = new UserDTO();

        //TODO default configuration should be taken from uaa.yml with specified default values of some properties is missing
        /*
         * security:
         *   idp:
         *     defaultAdditionalInformation:
         *       userIdentity: email
         *       userIdentityType: LOGIN.EMAIL
         *       firstName:  given_name
         *       lastName: family_name
         */

        //base info mapping
        userDTO.setFirstName((String) additionalInformation.get("given_name"));
        userDTO.setLastName((String) additionalInformation.get("family_name"));
        //login mapping
        UserLogin emailUserLogin = new UserLogin();
        emailUserLogin.setLogin(userIdentity);
        emailUserLogin.setTypeKey(UserLoginType.EMAIL.getValue());

        userDTO.setLogins(List.of(emailUserLogin));
        return userDTO;
    }

    @LogicExtensionPoint(value = "MapIdpRole")
    public String mapIdpRole(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        TenantProperties tenantProps = tenantPropertiesService.getTenantProps();
        TenantProperties.Security security = tenantProps.getSecurity();

        if (security == null) {
            throw new TenantNotProvidedException("Default role for tenant " + getTenantKey() + " not specified.");
        }
        return security.getDefaultUserRole();
    }

    private String getTenantKey() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantPropertiesService.getTenantContextHolder());
    }

}
