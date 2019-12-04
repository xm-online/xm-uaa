package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.domain.TemplateParams;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.lep.keyresolver.LdapSearchByTemplateKeyResolver;
import com.icthh.xm.uaa.service.exceptions.LdapServiceException;
import lombok.AllArgsConstructor;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

@LepService(group = "service.ldap")
@Service
@AllArgsConstructor
public class LdapService {

    private TenantPropertiesService tenantPropertiesService;

    @LogicExtensionPoint(value = "SearchByTemplate", resolver = LdapSearchByTemplateKeyResolver.class)
    @Transactional(readOnly = true)
    public Set<Map<String, List<String>>> searchByTemplate(String templateKey,
                                                           TemplateParams templateParams) {

        if (isNull(templateParams) || isNull(templateParams.getTemplateParams())) {
            throw new LdapServiceException("error.template.params.empty", "Template params is empty");
        }

        Object[] params = templateParams.getTemplateParams().toArray();

        List<TenantProperties.LdapSearchTemplate> searchTemplates = tenantPropertiesService.getTenantProps()
                                                                                           .getLdapSearchTemplates();

        TenantProperties.LdapSearchTemplate searchTemplate =
            searchTemplates
            .stream()
            .filter(template -> templateKey.equals(template.getTemplateKey()))
            .findFirst()
            .orElseThrow(() -> new LdapServiceException(
                "error.templateKey.not.found",
                "Search template with templateKey: " + templateKey + " not found")
            );

        TenantProperties.Ldap ldap = foundLdap(searchTemplate.getDomain());

        SpringSecurityLdapTemplate springSecurityLdapTemplate = getSpringSecurityLdapTemplate(ldap);

        return springSecurityLdapTemplate.searchForMultipleAttributeValues(ldap.getRootDn(),
                                                                           searchTemplate.getQuery(),
                                                                           params,
                                                                           searchTemplate.getAttributeNames());
    }

    private SpringSecurityLdapTemplate getSpringSecurityLdapTemplate(TenantProperties.Ldap ldap) {
        DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(ldap.getProviderUrl());
        ctx.setUserDn(ldap.getSystemUser());
        ctx.setPassword(ldap.getSystemPassword());
        ctx.afterPropertiesSet();

        return new SpringSecurityLdapTemplate(ctx);
    }

    private TenantProperties.Ldap foundLdap(String ldapDomain) {
        List<TenantProperties.Ldap> ldapProperties = tenantPropertiesService.getTenantProps().getLdap();

        return ldapProperties
            .stream()
            .filter(ldap -> ldapDomain.equals(ldap.getDomain()))
            .findFirst()
            .orElseThrow(() -> new LdapServiceException(
                "error.ldap.domain.not.found",
                "Ldap with domain: " + ldapDomain + " not found")
            );
    }
}
