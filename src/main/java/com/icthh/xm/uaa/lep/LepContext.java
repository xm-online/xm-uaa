package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.lep.spring.lepservice.LepServiceFactory;
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
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.web.client.RestTemplate;

public class LepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    public Object methodResult;

    public LepServiceFactory lepServices;
    public LepServices services;
    public LepTemplates templates;

    public static class LepServices {
        UserLoginService userLoginService;
        ClientService clientService;
        UserService userService;
        MailService mailService;
        AccountService accountService;
        TenantConfigService tenantConfigService;
        ProfileEventProducer profileEventProducer;
        CustomizableLepTokenStorage customizableTokeStorage;
        CustomAuthorizationCodeServices customAuthorizationCodeServices;
        LdapService ldapService;
        UserDetailsService userDetailsService;
        TenantPropertiesService tenantPropertiesService;
        LepTokenGranter lepTokenGranter;
        ClientDetailsService clientDetailsService;
    }

    public static class LepTemplates {
        public RestTemplate rest;
        public KafkaTemplateService kafka;
    }

}
