package com.icthh.xm.uaa.service.mail;

import static com.icthh.xm.uaa.config.Constants.TRANSLATION_KEY;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.forLanguageTag;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
import static org.springframework.context.i18n.LocaleContextHolder.getLocaleContext;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.context.i18n.LocaleContextHolder.setLocaleContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.messaging.communication.CommunicationMessageBuilder;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.jhipster.config.JHipsterProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/**
 * Service for sending emails.
 * We use the @Async annotation to send emails asynchronously.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class MailService {

    public static final String PASSWORD_RESET_EMAIL_TEMPLATE = "passwordResetEmail";
    public static final String CREATION_EMAIL_TEMPLATE = "creationEmail";
    public static final String ACTIVATION_EMAIL_TEMPLATE = "activationEmail";
    public static final String PASSWORD_CHANGED_EMAIL_TEMPLATE = "passwordChangedEmail";

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String FROM = "from";
    private static final String EMAIL_TYPE = "TemplatedEmail";

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";
    private static final String TENANT_KEY_VALUE = "tenant";

    private final JHipsterProperties jHipsterProperties;
    private final MailProviderService mailProviderService;
    private final MessageSource messageSource;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final Configuration freeMarker;
    private final TenantContextHolder tenantContextHolder;
    private final TenantConfigService tenantConfigService;
    private final LocalizationMessageService localizationMessageService;
    private final CommunicationService communicationService;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties applicationProperties;
    private final TenantPropertiesService tenantPropertiesService;

    @Resource
    @Lazy
    private MailService selfReference;

    private static void execForCustomRid(String rid, Runnable runnable) {
        final String oldRid = MdcUtils.getRid();
        try {
            MdcUtils.putRid(rid);
            runnable.run();
        } finally {
            if (oldRid != null) {
                MdcUtils.putRid(oldRid);
            } else {
                MdcUtils.removeRid();
            }
        }
    }

    /**
     * Send activation email.
     */
    @Async
    public void sendActivationEmail(User user, String applicationUrl, TenantKey tenantKey, String rid) {
        execForCustomRid(rid, () -> {
            String email = user.getEmail();
            log.info("Sending activation email to {}", email);

            Map<String, Object> objectModel = new HashMap<>();
            objectModel.put(USER, user);
            objectModel.put(BASE_URL, applicationUrl);
            objectModel.put(TENANT_KEY_VALUE, tenantKey.getValue());

            sendEmailFromTemplate(
                tenantKey,
                user,
                ACTIVATION_EMAIL_TEMPLATE,
                "email.activation.title",
                email,
                objectModel
            );
        });
    }


    /**
     * Send creation email.
     */
    @Async
    public void sendCreationEmail(User user, String applicationUrl, TenantKey tenantKey, String rid) {
        execForCustomRid(rid, () -> {
            log.info("Sending creation email to '{}'", user.getEmail());

            Map<String, Object> objectModel = new HashMap<>();
            objectModel.put(USER, user);
            objectModel.put(BASE_URL, applicationUrl);
            objectModel.put(TENANT_KEY_VALUE, tenantKey.getValue());

            sendEmailFromTemplate(
                tenantKey,
                user,
                CREATION_EMAIL_TEMPLATE,
                "email.activation.title",
                user.getEmail(),
                objectModel
            );
        });
    }

    /**
     * Send password reset email.
     *
     * @param user           object which stores info about user
     * @param applicationUrl application url
     * @param tenantKey      tenant key
     * @param rid            request/transaction id (used for logging)
     */
    @Async
    public void sendPasswordResetMail(User user, String applicationUrl, TenantKey tenantKey, String rid) {
        execForCustomRid(rid, () -> {
            log.info("Sending password reset email to '{}'", user.getEmail());
            Map<String, Object> objectModel = new HashMap<>();
            objectModel.put(USER, user);
            objectModel.put(BASE_URL, applicationUrl);
            objectModel.put(TENANT_KEY_VALUE, tenantKey.getValue());

            sendEmailFromTemplate(
                tenantKey,
                user,
                PASSWORD_RESET_EMAIL_TEMPLATE,
                "email.reset.title",
                user.getEmail(),
                objectModel
            );

        });
    }

    /**
     * Send password changed email.
     *
     * @param user           object which stores info about user
     * @param applicationUrl application url
     * @param tenantKey      tenant key
     * @param rid            request/transaction id (used for logging)
     */
    @Async
    public void sendPasswordChangedMail(User user, String applicationUrl, TenantKey tenantKey, String rid) {
        execForCustomRid(rid, () -> {
            log.info("Sending password changed email to '{}'", user.getEmail());
            Map<String, Object> objectModel = new HashMap<>();
            objectModel.put(USER, user);
            objectModel.put(BASE_URL, applicationUrl);
            objectModel.put(TENANT_KEY_VALUE, tenantKey.getValue());

            sendEmailFromTemplate(
                tenantKey,
                user,
                PASSWORD_CHANGED_EMAIL_TEMPLATE,
                "email.changed.title",
                user.getEmail(),
                objectModel
            );
        });
    }

    public void sendEmailFromTemplate(User user,
                                      String templateName,
                                      String subject,
                                      String email,
                                      String from,
                                      Map<String, Object> objectModel) {
        selfReference.sendEmailFromTemplateAsync(tenantContextHolder.getContext().getTenantKey().get(),
            user,
            templateName,
            subject,
            email,
            from,
            objectModel,
            MdcUtils.getRid());
    }

    @Async
    protected void sendEmailFromTemplateAsync(TenantKey tenantKey,
                                              User user,
                                              String templateName,
                                              String titleKey,
                                              String email,
                                              String from,
                                              Map<String, Object> objectModel,
                                              String rid) {
        execForCustomRid(rid, () -> {
            this.sendEmailFromTemplate(tenantKey, user, templateName, titleKey, email, from, objectModel);
        });
    }

    @Async
    public void sendEmailFromTemplate(TenantKey tenantKey,
                                      User user,
                                      String templateName,
                                      String titleKey,
                                      String email,
                                      Map<String, Object> model,
                                      String rid) {
        execForCustomRid(rid, () -> {
            String subject = messageSource.getMessage(titleKey, null, forLanguageTag(user.getLangKey()));
            sendEmailFromTemplate(tenantKey, user, templateName, subject, email, generateFrom(tenantKey), model);
        });
    }

    // package level for testing
    void sendEmailFromTemplate(TenantKey tenantKey,
                               User user,
                               String templateName,
                               String titleKey,
                               String email,
                               Map<String, Object> objectModel) {
        sendEmailFromTemplate(tenantKey, user, templateName, titleKey, email, generateFrom(tenantKey), objectModel);
    }

    private void sendEmailFromTemplate(TenantKey tenantKey,
                                       User user,
                                       String templateName,
                                       String titleKey,
                                       String email,
                                       String from,
                                       Map<String, Object> objectModel) {
        if (isCommunicationEnabled(tenantKey)) {
            String langKey = user.getLangKey();
            if (isSystemEmail(templateName)) {
                sendCommunicationEmailEvent(tenantKey, langKey, templateName, null, email, from, objectModel);
            } else {
                Locale locale = forLanguageTag(langKey);
                String subject = messageSource.getMessage(titleKey, null, locale);
                sendCommunicationEmailEvent(tenantKey, langKey, templateName, subject, email, from, objectModel);
            }
        } else {
            sendEmailFromTemplateByMailSender(tenantKey, user, templateName, titleKey, email, from, objectModel);
        }
    }

    private boolean isCommunicationEnabled(TenantKey tenantKey) {
        return Optional.ofNullable(tenantPropertiesService.getTenantProps(tenantKey))
            .map(TenantProperties::getCommunication)
            .map(TenantProperties.Communication::getEnabled)
            .map(Boolean.TRUE::equals)
            .orElse(applicationProperties.getCommunication() != null
                && applicationProperties.getCommunication().isEnabled());
    }

    private void sendCommunicationEmailEvent(TenantKey tenantKey,
                                             String langKey,
                                             String templateName,
                                             String subject,
                                             String email,
                                             String from,
                                             Map<String, Object> objectModel) {
        runWithTenantContext(tenantKey, () -> {
            CommunicationMessage communicationMessage = new CommunicationMessage();
            communicationMessage.setCharacteristic(new ArrayList<>());
            communicationMessage.setSubject(subject);
            communicationMessage.setType(EMAIL_TYPE);
            communicationMessage = new CommunicationMessageBuilder(communicationMessage, objectMapper)
                .addSenderId(from)
                .addReceiverEmail(email)
                .addLanguage(langKey)
                .addTemplateName(templateName)
                .addTemplateModel(objectModel)
                .build();

            communicationService.sendEmailEvent(communicationMessage);
        });
    }

    private void sendEmailFromTemplateByMailSender(TenantKey tenantKey,
                                                   User user,
                                                   String templateName,
                                                   String titleKey,
                                                   String email,
                                                   String from,
                                                   Map<String, Object> objectModel) {
        if (email == null) {
            log.warn("Can't send email on null address for tenant: {}, user key: {}, email template: {}",
                tenantKey.getValue(),
                user.getUserKey(),
                templateName);
            return;
        }

        String emailTemplate = tenantEmailTemplateService.getEmailTemplate(tenantKey.getValue(), user.getLangKey(), templateName);
        Locale locale = forLanguageTag(user.getLangKey());

        try {
            tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(tenantKey));

            String templateKey = EmailTemplateUtil.emailTemplateKey(tenantKey, user.getLangKey(), templateName);
            Template mailTemplate = new Template(templateKey, emailTemplate, freeMarker);
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, objectModel);
            String subject = messageSource.getMessage(titleKey, null, locale);
            sendEmail(
                email,
                resolve(SUBJECT, subject, templateName, locale),
                content,
                resolve(FROM, from, templateName, locale),
                mailProviderService.getJavaMailSender(tenantKey.getValue()));
        } catch (TemplateException e) {
            throw new IllegalStateException("Mail template rendering failed");
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading mail template");
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }

    private String generateFrom(TenantKey tenantKey) {
        return jHipsterProperties.getMail().getFrom().replace("<tenantname>", tenantKey.getValue());
    }

    private String resolve(String key, String defaultValue, String templateName, Locale locale) {
        Object settings = tenantConfigService.getConfig().get(MAIL_SETTINGS);
        LocaleContext localeContext = getLocaleContext();
        setLocale(locale);

        String result = Optional.ofNullable(settings)
            .filter(List.class::isInstance).map(it -> (List<Object>) it)
            .flatMap(mailSettings ->
                mailSettings.stream()
                    .filter(Map.class::isInstance).map(it -> (Map<String, Object>) it)
                    // find by templateName
                    .filter(it -> templateName.equals(it.get(TEMPLATE_NAME)))
                    .findFirst()
                    // get from of subject or etc it exists...
                    .filter(it -> it.containsKey(key))
                    .map(it -> it.get(key))
                    // get localized name
                    .filter(Map.class::isInstance).map(it -> (Map<String, String>) it)
                    .flatMap(this::getI18nName)
            ).orElse(defaultValue);

        setLocaleContext(localeContext);
        return result;
    }

    private Optional<String> getI18nName(Map<String, String> name) {
        if (name.containsKey(TRANSLATION_KEY)) {
            String translationKey = name.get(TRANSLATION_KEY);
            return Optional.of(localizationMessageService.getMessage(translationKey));
        } else if (name.containsKey(getLocale().getLanguage())) {
            return Optional.of(name.get(getLocale().getLanguage()));
        } else if (name.containsKey(ENGLISH.getLanguage())) {
            return Optional.of(name.get(ENGLISH.getLanguage()));
        }
        return Optional.empty();
    }

    // package level for testing
    void sendEmail(String to, String subject, String content, String from, JavaMailSender javaMailSender) {
        log.debug("Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            false, true, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage,
                false, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(subject);
            message.setText(content, true);
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Email could not be sent to user '{}'", to, e);
            } else {
                log.warn("Email could not be sent to user '{}': {}", to, e.getMessage());
            }
        }
    }

    private void runWithTenantContext(TenantKey tenantKey, Runnable runnable) {
        try {
            tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(tenantKey));
            runnable.run();
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }

    private boolean isSystemEmail(String templateName) {
        return ACTIVATION_EMAIL_TEMPLATE.equals(templateName)
            || PASSWORD_CHANGED_EMAIL_TEMPLATE.equals(templateName)
            || PASSWORD_RESET_EMAIL_TEMPLATE.equals(templateName)
            || CREATION_EMAIL_TEMPLATE.equals(templateName);
    }
}
