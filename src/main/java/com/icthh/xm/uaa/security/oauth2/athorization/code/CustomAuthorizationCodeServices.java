package com.icthh.xm.uaa.security.oauth2.athorization.code;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
@LepService(group = "service.auth")
public class CustomAuthorizationCodeServices implements AuthorizationCodeServices {

    private final InMemoryAuthorizationCodeServices defaultInMemoryAuthorizationCodeServices
        = new InMemoryAuthorizationCodeServices();

    @Override
    @LogicExtensionPoint("CreateAuthorizationCode")
    public String createAuthorizationCode(OAuth2Authentication authentication) {
        return defaultInMemoryAuthorizationCodeServices.createAuthorizationCode(authentication);
    }

    @Override
    @LogicExtensionPoint("ConsumeAuthorizationCode")
    public OAuth2Authentication consumeAuthorizationCode(String code) {
        return defaultInMemoryAuthorizationCodeServices.remove(code);
    }
}
