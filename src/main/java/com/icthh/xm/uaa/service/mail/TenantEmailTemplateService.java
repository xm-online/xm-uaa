package com.icthh.xm.uaa.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import freemarker.cache.StringTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing email template.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantEmailTemplateService implements RefreshableConfiguration {

    private static final String FILE_NAME = "fileName";
    private static final String LANG_KEY = "langKey";
    private static final String TENANT_NAME = "tenantName";
    private static final String DEFAULT_LANG = "en";

    private ConcurrentHashMap<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ApplicationProperties applicationProperties;
    private final StringTemplateLoader templateLoader;

    /**
     * Search email template by email template key.
     *
     * @param tenantKey    tenant key
     * @param langKey      language key
     * @param templateName template name
     * @return email template
     */
    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplate(String tenantKey, String langKey, String templateName) {
        String emailTemplateKey = EmailTemplateUtil.emailTemplateKey(tenantKey, langKey, templateName);
        String defaultEmailTemplateKey = EmailTemplateUtil.emailTemplateKey(tenantKey, DEFAULT_LANG, templateName);
        if (!emailTemplates.containsKey(emailTemplateKey) && !emailTemplates.containsKey(defaultEmailTemplateKey)) {
            throw new IllegalArgumentException("Email template was not found");
        }
        return emailTemplates.getOrDefault(emailTemplateKey, emailTemplates.get(defaultEmailTemplateKey));
    }

    @Override
    public void onRefresh(String key, String config) {
        String pathPattern = applicationProperties.getEmailPathPattern();

        String tenantKeyValue = matcher.extractUriTemplateVariables(pathPattern, key).get(TENANT_NAME);
        String langKey = matcher.extractUriTemplateVariables(pathPattern, key).get(LANG_KEY);
        String templateName = matcher.extractUriTemplateVariables(pathPattern, key).get(FILE_NAME);

        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue),
                                                                langKey,
                                                                templateName);
        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templateKey);
            templateLoader.removeTemplate(templateKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templateName,
                            langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templateKey, config);
            templateLoader.putTemplate(templateKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templateName,
                            langKey, tenantKeyValue);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getEmailPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
