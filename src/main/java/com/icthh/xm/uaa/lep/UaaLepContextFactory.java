package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepContextFactory;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.CustomizableLepTokenStorage;
import com.icthh.xm.uaa.security.oauth2.LepTokenGranter;
import com.icthh.xm.uaa.security.oauth2.athorization.code.CustomAuthorizationCodeServices;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.LdapService;
import com.icthh.xm.uaa.service.SeparateTransactionExecutor;
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

@Component
@RequiredArgsConstructor
public class UaaLepContextFactory implements LepContextFactory {

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
    private final KafkaTemplateService kafka;
    private final PermissionCheckService permissionCheckService;
    private final SeparateTransactionExecutor separateTransactionExecutor;
    private final CommunicationService communicationService;

    @Override
    public BaseLepContext buildLepContext(LepMethod lepMethod) {
        LepContext lepContext = new LepContext();

        lepContext.services = new LepContext.LepServices();
        lepContext.services.mailService = mailService;
        lepContext.services.userService = userService;
        lepContext.services.clientService = clientService;
        lepContext.services.accountService = accountService;
        lepContext.services.userLoginService = userLoginService;
        lepContext.services.tenantConfigService = tenantConfigService;
        lepContext.services.profileEventProducer = profileEventProducer;
        lepContext.services.customizableTokeStorage = customizableLepTokenStorage;
        lepContext.services.customAuthorizationCodeServices = customAuthorizationCodeServices;
        lepContext.services.ldapService = ldapService;
        lepContext.services.userDetailsService = userDetailsService;
        lepContext.services.tenantPropertiesService = tenantPropertiesService;
        lepContext.services.lepTokenGranter = lepTokenGranter;
        lepContext.services.clientDetailsService = clientDetailsService;
        lepContext.services.permissionCheckService = permissionCheckService;
        lepContext.services.separateTransactionExecutor = separateTransactionExecutor;
        lepContext.services.communicationService = communicationService;

        lepContext.templates = new LepContext.LepTemplates();
        lepContext.templates.rest = restTemplate;
        lepContext.templates.kafka = kafka;

        return lepContext;
    }
}
