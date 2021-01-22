package com.icthh.xm.uaa.security.oauth2.idp;

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
import org.springframework.security.oauth2.provider.token.store.IssuerClaimVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
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
        String idpToken = parameters.remove("token");
        // parse IDP id token
        OAuth2AccessToken idpOAuth2IdToken = jwkTokenStore.readAccessToken(idpToken);

        validateAccessToken(idpOAuth2IdToken);

        //TODO think about LEP + config
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();

        //user + role section
        //TODO think about LEP + config
        DomainUserDetails userDetails = retrieveDomainUserDetails(additionalInformation);
        Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapAuthorities(userDetails.getAuthorities());

        //build container for user
        IdpAuthenticationToken userAuthenticationToken = new IdpAuthenticationToken(userDetails, null, authorities);
        userAuthenticationToken.setDetails(parameters);

        return userAuthenticationToken;
    }

    //TODO LEP
    private void buildClaimsValidators() {
//        Map<String, Set<JwtClaimsSetVerifier>> claimsSetVerifiers = jwkTokenStore.getJwtTokenEnhancer().getJwtClaimsSetVerifiers();
//
//        Set<JwtClaimsSetVerifier> jwtClaimsSetVerifiers = Set.of(IssuerClaimVerifier);
//        claimsSetVerifiers.put();
    }

    private DomainUserDetails retrieveDomainUserDetails(Map<String, Object> additionalInformation) {
        String userEmail = (String) additionalInformation.get("email");
        DomainUserDetails userDetails = domainUserDetailsService.retrieveUserByUsername(userEmail);

        if (userDetails == null) {
            return buildDomainUserDetails(additionalInformation, userEmail);
        }

        return userDetails;
    }

    private DomainUserDetails buildDomainUserDetails(Map<String, Object> additionalInformation, String userEmail) {
        log.debug("User with login: {} not exists. Creating new user.", userEmail);

        User newUser = createUser(additionalInformation);

        UserLogin userLogin = newUser.getLogins()
            .stream()
            .filter(login -> UserLoginType.EMAIL.getValue().equals(login.getTypeKey()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("UserLogin with type " +
                UserLoginType.EMAIL.getValue() + "not found"));

        return DomainUserDetailsService.buildDomainUserDetails(userEmail, getTenantKey(), userLogin);
    }

    private User createUser(Map<String, Object> additionalInformation) {
        UserDTO userDTO = buildUserDTO(additionalInformation);

        userLoginService.normalizeLogins(userDTO.getLogins());
        userLoginService.verifyLoginsNotExist(userDTO.getLogins());

        return userService.createUser(userDTO);
    }

    //TODO add claim validation: audience and issuer
    private void validateAccessToken(OAuth2AccessToken idpOAuth2IdToken) {
        //validate issuer and audience
    }

    private UserDTO buildUserDTO(Map<String, Object> additionalInformation) {
        UserDTO userDTO = new UserDTO();

        //base info mapping
        userDTO.setFirstName((String) additionalInformation.get("given_name"));
        userDTO.setLastName((String) additionalInformation.get("family_name"));
        //login mapping
        UserLogin emailUserLogin = new UserLogin();
        emailUserLogin.setLogin((String) additionalInformation.get("email"));
        emailUserLogin.setTypeKey(UserLoginType.EMAIL.getValue());

        userDTO.setLogins(List.of(emailUserLogin));
        userDTO.setRoleKey(getDefaultUserRoleProp());

        return userDTO;
    }

    private String getDefaultUserRoleProp() {
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
