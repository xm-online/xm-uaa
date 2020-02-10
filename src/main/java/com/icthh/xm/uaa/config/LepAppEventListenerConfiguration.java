package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.uaa.lep.XmUaaLepProcessingApplicationListener;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.oauth2.athorization.code.CustomAuthorizationCodeServices;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;

/**
 * The {@link LepAppEventListenerConfiguration} class.
 */
@Configuration
public class LepAppEventListenerConfiguration {

    @Bean
    XmUaaLepProcessingApplicationListener buildLepProcessingApplicationListener(
        AccountService accountService,
        TenantConfigService tenantConfigService,
        UserLoginService userLoginService,
        ProfileEventProducer profileEventProducer,
        MailService mailService,
        @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
        UserService userService,
        CommonsService commonsService,
        CustomAuthorizationCodeServices customAuthorizationCodeServices,
        JwtTokenStore jwtTokenStore

    ) {
        return new XmUaaLepProcessingApplicationListener(
            jwtTokenStore,
            mailService,
            userService,
            restTemplate,
            commonsService,
            accountService,
            userLoginService,
            tenantConfigService,
            profileEventProducer,
            customAuthorizationCodeServices
        );
    }
}
