package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.lep.BaseProceedingLep;
import com.icthh.xm.commons.lep.commons.CommonsService;
import com.icthh.xm.commons.lep.spring.LepThreadHelper;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.tenant.TenantContext;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_PROFILE_EVEBT_PRODUCER_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_ACCOUNT;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_CLIENT;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_CLIENT_DETAILS_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_CUSTOMIZABLE_TOKE_STORAGE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_CUSTOM_AUTHORIZATION_CODE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_LDAP_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_LEP_TOKEN_GRANTER;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_MAIL;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_TENANT_CONFIG_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_TENANT_PROPERTIES_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_USER;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_USER_DETAILS_SERVICE;
import static com.icthh.xm.uaa.lep.XmUaaLepConstants.BINDING_SUB_KEY_SERVICE_USER_LOGIN_SERVICE;

public class LepContext {

    public Object commons;
    public Object inArgs;
    public BaseProceedingLep lep;
    public LepThreadHelper thread;
    public XmAuthenticationContext authContext;
    public TenantContext tenantContext;
    public Object methodResult;

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
    }

}
