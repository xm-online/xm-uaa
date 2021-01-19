package com.icthh.xm.uaa.security.oauth2.idp;

import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.IdpAuthenticationToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
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

@Slf4j
public class IdpTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "idp_token";

    private final CustomJwkTokenStore jwkTokenStore;
    private final UserDetailsService domainUserDetailsService;
    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();


    public IdpTokenGranter(AuthorizationServerTokenServices tokenServices,
                           ClientDetailsService clientDetailsService,
                           OAuth2RequestFactory requestFactory,
                           CustomJwkTokenStore jwkTokenStore, UserDetailsService domainUserDetailsService) {
        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
        this.jwkTokenStore = jwkTokenStore;
        this.domainUserDetailsService = domainUserDetailsService;
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

        String idpToken = parameters.remove("token");
        // parse IDP id token
        OAuth2AccessToken idpOAuth2IdToken = jwkTokenStore.readAccessToken(idpToken);

        validateAccessToken(idpOAuth2IdToken);


        //TODO think about LEP + config
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        String userEmail = (String) additionalInformation.get("email");

        //user + role section
        //TODO think about LEP + config
        DomainUserDetails userDetails = (DomainUserDetails) domainUserDetailsService.loadUserByUsername(userEmail);
        Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapAuthorities(userDetails.getAuthorities());

        //build container for user
        IdpAuthenticationToken userAuthenticationToken = new IdpAuthenticationToken(userDetails, null, authorities);
        userAuthenticationToken.setDetails(parameters);

        return userAuthenticationToken;

    }

    //TODO add claim validation: audience and issuer
    private void validateAccessToken(OAuth2AccessToken idpOAuth2IdToken) {
        //validate issuer and audience


        //parse token using JWTParser.parse(token)
//        JWT jwt = JWTParser.parse(token);
        //extract issuer and audience
//        JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
//        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();
        // get issuer
//        claims.get("iss");
//        Object audiences = claims.get("aud");
//        if (audiences != null) {
//            List<Map<String, List<String>>> aud = (List<Map<String, List<String>>>) audiences;
//            aud.stream().flatMap(Collection::stream);
    }

}
