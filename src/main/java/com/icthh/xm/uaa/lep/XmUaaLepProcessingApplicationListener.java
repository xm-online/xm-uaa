package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.commons.CommonsExecutor;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.commons.lep.spring.SpringLepProcessingApplicationListener;
import com.icthh.xm.lep.api.ScopedContext;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.CustomizableLepTokenStorage;
import com.icthh.xm.uaa.security.oauth2.LepTokenGranter;
import com.icthh.xm.uaa.security.oauth2.athorization.code.CustomAuthorizationCodeServices;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.LdapService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.lep.XmUaaLepConstants.*;

/**
 * The {@link XmUaaLepProcessingApplicationListener} class.
 */
@RequiredArgsConstructor
@Component
public class XmUaaLepProcessingApplicationListener extends SpringLepProcessingApplicationListener {

    private final MailService mailService;
    private final UserService userService;
    private final ClientService clientService;
    @Qualifier("loadBalancedRestTemplate")
    private final RestTemplate restTemplate;
    private final CommonsService commonsService;
    private final AccountService accountService;
    private final UserLoginService userLoginService;
    private final TenantConfigService tenantConfigService;
    private final ProfileEventProducer profileEventProducer;
    private final CustomizableLepTokenStorage customizableLepTokenStorage;
    private final CustomAuthorizationCodeServices customAuthorizationCodeServices;
    private final LdapService ldapService;
    private final UserDetailsService userDetailsService;
    private final TenantPropertiesService tenantPropertiesService;
    private final LepTokenGranter lepTokenGranter;
    private final ClientDetailsService clientDetailsService;
    private final KafkaTemplateService kafkaTemplateService;
    private final PermissionCheckService permissionCheckService;

    @Override
    protected void bindExecutionContext(final ScopedContext executionContext) {

        Map<String, Object> services = new HashMap<>();

        services.put(BINDING_SUB_KEY_SERVICE_CLIENT, clientService);
        services.put(BINDING_SUB_KEY_SERVICE_USER, userService);
        services.put(BINDING_SUB_KEY_SERVICE_MAIL, mailService);
        services.put(BINDING_SUB_KEY_SERVICE_ACCOUNT, accountService);
        services.put(BINDING_SUB_KEY_SERVICE_USER_LOGIN_SERVICE, userLoginService);
        services.put(BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERVICE, tenantConfigService);
        services.put(BINDING_SUB_KEY_PROFILE_EVEBT_PRODUCER_SERVICE, profileEventProducer);
        services.put(BINDING_SUB_KEY_SERVICE_CUSTOMIZABLE_TOKE_STORAGE, customizableLepTokenStorage);
        services.put(BINDING_SUB_KEY_SERVICE_CUSTOM_AUTHORIZATION_CODE, customAuthorizationCodeServices);
        services.put(BINDING_SUB_KEY_SERVICE_LDAP_SERVICE, ldapService);
        services.put(BINDING_SUB_KEY_SERVICE_USER_DETAILS_SERVICE, userDetailsService);
        services.put(BINDING_SUB_KEY_SERVICE_TENANT_PROPERTIES_SERVICE, tenantPropertiesService);
        services.put(BINDING_SUB_KEY_SERVICE_LEP_TOKEN_GRANTER, lepTokenGranter);
        services.put(BINDING_SUB_KEY_SERVICE_CLIENT_DETAILS_SERVICE, clientDetailsService);
        services.put(BINDING_SUB_KEY_PERMISSION_CHECK_SERVICE, permissionCheckService);

        executionContext.setValue(BINDING_KEY_COMMONS, new CommonsExecutor(commonsService));
        executionContext.setValue(BINDING_KEY_SERVICES, services);

        Map<String, Object> templates = new HashMap<>();
        templates.put(BINDING_SUB_KEY_TEMPLATE_REST, restTemplate);
        templates.put(BINDING_SUB_KEY_TEMPLATE_KAFKA, kafkaTemplateService);

        executionContext.setValue(BINDING_KEY_TEMPLATES, templates);

        // other beans to be passed into LEP execution context?
    }
}
