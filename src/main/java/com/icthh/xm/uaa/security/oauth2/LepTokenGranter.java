package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.lep.keyresolver.LepTokenGranterKeyResolver;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

@Slf4j
@LepService(group = "security.oauth2")
@RequiredArgsConstructor
public class LepTokenGranter implements TokenGranter {

    private final ClientDetailsService clientDetailsService;
    private final AuthenticationManager authenticationManager;

    @Setter(onMethod = @__(@Autowired))
    private LepTokenGranter self;

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        String clientId = tokenRequest.getClientId();
        ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
        if (!validateGrantType(grantType, client)) {
            return null;
        }
        return self.grantSelf(grantType, tokenRequest);
    }

    @LogicExtensionPoint(value = "TokenGranter", resolver = LepTokenGranterKeyResolver.class)
    public OAuth2AccessToken grantSelf(String grantType, TokenRequest tokenRequest) {
        return null;
    }

    public Authentication authenticate(ClientDetails client, TokenRequest tokenRequest) {
        return authenticationManager.authenticate(getOAuth2Authentication(client, tokenRequest));
    }

    public OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Request storedOAuth2Request = new DefaultOAuth2RequestFactory(clientDetailsService).
            createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, null);
    }

    private boolean validateGrantType(String grantType, ClientDetails clientDetails) {
        Collection<String> authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        if (authorizedGrantTypes != null && !authorizedGrantTypes.isEmpty()
            && !authorizedGrantTypes.contains(grantType)) {
            return false;
        }
        return true;
    }

}
