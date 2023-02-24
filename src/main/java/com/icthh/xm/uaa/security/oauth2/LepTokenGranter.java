package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.lep.keyresolver.LepTokenGranterKeyResolver;
import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

@Slf4j
@LepService(group = "security.oauth2")
@RequiredArgsConstructor
public class LepTokenGranter implements TokenGranter {

    private final ClientDetailsService clientDetailsService;
    @Getter
    private final AuthenticationManager authenticationManager;
    @Getter
    private final AuthorizationServerTokenServices tokenService;

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

    private boolean validateGrantType(String grantType, ClientDetails clientDetails) {
        Collection<String> authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        if (authorizedGrantTypes != null && !authorizedGrantTypes.isEmpty()
            && !authorizedGrantTypes.contains(grantType)) {
            return false;
        }
        return true;
    }

}
