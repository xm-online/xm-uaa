package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.domainevent.outbox.service.OutboxTransportService;
import com.icthh.xm.commons.domainevent.outbox.service.OutboxTransportService.OutboxTransportServiceField;
import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
import com.icthh.xm.commons.logging.trace.TraceService;
import com.icthh.xm.commons.logging.trace.TraceService.TraceServiceField;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.web.client.RestTemplate;

public class LepContext extends BaseLepContext implements TraceServiceField, OutboxTransportServiceField {

    public LepServices services;
    public LepTemplates templates;

    public static class LepServices {
        public UserLoginService userLoginService;
        public ClientService clientService;
        public UserService userService;
        public MailService mailService;
        public AccountService accountService;
        public TenantConfigService tenantConfigService;
        public ProfileEventProducer profileEventProducer;
        public CustomizableLepTokenStorage customizableTokeStorage; // typo can't be fixed by backward compatibility
        public CustomAuthorizationCodeServices customAuthorizationCodeServices;
        public LdapService ldapService;
        public UserDetailsService userDetailsService;
        public TenantPropertiesService tenantPropertiesService;
        public LepTokenGranter lepTokenGranter;
        public ClientDetailsService clientDetailsService;
        public PermissionCheckService permissionCheckService;
        public SeparateTransactionExecutor separateTransactionExecutor;
        public CommunicationService communicationService;
    }

    public static class LepTemplates {
        public RestTemplate rest;
        public KafkaTemplateService kafka;
    }

}
