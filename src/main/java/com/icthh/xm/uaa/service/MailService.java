package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.User;
import io.github.jhipster.config.JHipsterProperties;
import java.util.Locale;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

/**
 * Service for sending emails.
 *
 * <p>We use the @Async annotation to send emails asynchronously.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class MailService {

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";
    private static final String TENANT = "tenant";

    private final JHipsterProperties jHipsterProperties;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    /**
     * Send activation email.
     */
    @Async
    public void sendActivationEmail(User user, String tenant, String url, String rid) {
        MDCUtil.putRid(rid);
        String email = user.getEmail();
        log.info("Sending activation email to {}", email);

        sendEmailFromTemplate(user,
            "activationEmail",
            "email.activation.title",
            generateFrom(tenant),
            email, url, tenant);
    }

    void sendEmail(String to, String subject, String content, String from) {
        log.debug("Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            false, true, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, CharEncoding.UTF_8);
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

    void sendEmailFromTemplate(User user, String templateName, String titleKey,
                                       String from, String email, String url, String tenant) {
        if (email != null) {
            Locale locale = Locale.forLanguageTag(user.getLangKey());
            Context context = new Context(locale);
            context.setVariable(USER, user);
            context.setVariable(BASE_URL, url);
            context.setVariable(TENANT, tenant);
            String content = templateEngine.process(templateName, context);
            String subject = messageSource.getMessage(titleKey, null, locale);
            sendEmail(email, subject, content, from);
        }
    }

    @Async
    public void sendCreationEmail(User user, String url, String tenantName, String rid) {
        MDCUtil.putRid(rid);
        log.info("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplate(
            user,
            "creationEmail",
            "email.activation.title",
            generateFrom(tenantName),
            user.getEmail(),
            url,
            tenantName
        );
    }

    /**
     * Send password reset email.
     *
     * @param user object which stores info about user
     * @param url application url
     * @param tenantName tenant name
     * @param rid transaction id (use for logging)
     */
    @Async
    public void sendPasswordResetMail(User user, String url, String tenantName, String rid) {
        MDCUtil.putRid(rid);
        log.info("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplate(
            user,
            "passwordResetEmail",
            "email.reset.title",
            generateFrom(tenantName),
            user.getEmail(),
            url,
            tenantName
        );
    }

    /**
     * Send social registration validation email.
     */
    @Async
    public void sendSocialRegistrationValidationEmail(User user, String email, String provider, String tenantName, String rid) {
        MDCUtil.putRid(rid);
        log.info("Sending social registration validation email to {}", email);
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable("provider", StringUtils.capitalize(provider));
        String content = templateEngine.process("socialRegistrationValidationEmail", context);
        String subject = messageSource.getMessage("email.social.registration.title", null, locale);
        sendEmail(email, subject, content, generateFrom(tenantName));
    }

    private String generateFrom(String tenantName) {
        return jHipsterProperties.getMail().getFrom().replace("<tenantname>", tenantName);
    }

}
