package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.SpringLepProcessingApplicationListener;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.oauth2.athorization.code.CustomAuthorizationCodeServices;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_KEY_COMMONS;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_KEY_SERVICES;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_KEY_TEMPLATES;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_PROFILE_EVEBT_PRODUCER_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_ACCOUNT;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_CUSTOM_AUTHORIZATION_CODE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_JWT_TOKEN_STORE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_MAIL;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_USER;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_USER_LOGIN_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_TEMPLATE_REST;

/**
 * The {@link XmUaaLepProcessingApplicationListener} class.
 */
@RequiredArgsConstructor
public class XmUaaLepProcessingApplicationListener extends SpringLepProcessingApplicationListener {

    private final JwtTokenStore jwtTokeStore;
    private final MailService mailService;
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final CommonsService commonsService;
    private final AccountService accountService;
    private final UserLoginService userLoginService;
    private final TenantConfigService tenantConfigService;
    private final ProfileEventProducer profileEventProducer;
    private final CustomAuthorizationCodeServices customAuthorizationCodeServices;


    @Override
    protected void bindExecutionContext(final ScopedContext executionContext) {

        Map<String, Object> services = new HashMap<>();

        services.put(BINDING_SUB_KEY_SERVICE_USER, userService);
        services.put(BINDING_SUB_KEY_SERVICE_MAIL, mailService);
        services.put(BINDING_SUB_KEY_SERVICE_JWT_TOKEN_STORE, jwtTokeStore);
        services.put(BINDING_SUB_KEY_SERVICE_ACCOUNT, accountService);
        services.put(BINDING_SUB_KEY_SERVICE_USER_LOGIN_SERVICE, userLoginService);
        services.put(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERVICE, tenantConfigService);
        services.put(BINDING_SUB_KEY_PROFILE_EVEBT_PRODUCER_SERVICE, profileEventProducer);
        services.put(BINDING_SUB_KEY_SERVICE_CUSTOM_AUTHORIZATION_CODE, customAuthorizationCodeServices);

        executionContext.setValue(BINDING_KEY_COMMONS, new CommonsExecutor(commonsService));
        executionContext.setValue(BINDING_KEY_SERVICES, services);

        Map<String, Object> templates = new HashMap<>();
        templates.put(BINDING_SUB_KEY_TEMPLATE_REST, restTemplate);

        executionContext.setValue(BINDING_KEY_TEMPLATES, templates);

        // other beans to be passed into LEP execution context?
    }
}
