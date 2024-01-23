package com.icthh.xm.uaa.service.mail;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import freemarker.template.Configuration;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.uaa.config.Constants.TRANSLATION_KEY;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRANCE;
import static java.util.Locale.forLanguageTag;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MailServiceUnitTest {

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String FROM = "from";

    private static final String EMAIL_TEMPLATE = "EMAIL_TEMPLATE";
    private static final String TEST_TEMPLATE_CONTENT = "test template content";
    private static final String TENANT_KEY = "TENANT_KEY";
    private static final String MOCK_FROM = "MOCK_FROM";
    private static final String MOCK_SUBJECT = "MOCK_SUBJECT";
    private static final String TO = "to@yopmail.com";

    @InjectMocks
    private MailService mailService;

    private JavaMailSender javaMailSender = mock(JavaMailSender.class);

    @Spy
    private MailProviderService mailProviderService = new MailProviderService(javaMailSender);

    @Mock
    private TenantEmailTemplateService tenantEmailTemplateService;
    @Spy
    private Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_0);
    @Mock
    private TenantConfigService tenantConfigService;
    @Mock
    private LocalizationMessageService localizationMessageService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Communication appCommunication;
    @Mock
    private TenantPropertiesService tenantPropertiesService;

    @Test
    @SneakyThrows
    public void ifNoConfigReturnDefault() {
        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigMailSettingsNoListReturnDefault() {
        when(tenantConfigService.getConfig()).thenReturn(new HashMap<String, Object>() {{
            put(MAIL_SETTINGS, new HashMap<>());
        }});

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    private MimeMessage sendEmail() {
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(mock(PrivilegedTenantContext.class));
        when(messageSource.getMessage(MOCK_SUBJECT, null, forLanguageTag("fr"))).thenReturn(MOCK_SUBJECT);
        when(tenantEmailTemplateService.getEmailTemplate(TENANT_KEY, FRANCE.getLanguage(), EMAIL_TEMPLATE)).thenReturn(TEST_TEMPLATE_CONTENT);
        when(applicationProperties.getCommunication()).thenReturn(appCommunication);
        when(appCommunication.isEnabled()).thenReturn(FALSE);
        MimeMessage mock = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mock);

        User user = new User();
        user.setLangKey(FRANCE.getLanguage());

        mailService.sendEmailFromTemplateAsync(
            TenantKey.valueOf(TENANT_KEY),
            user,
            EMAIL_TEMPLATE,
            MOCK_SUBJECT,
            TO,
            MOCK_FROM,
            emptyMap(),
            "rid"
        );

        return mock;
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTemplateReturnDefault() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "OTHER_TEMPLATE",
                SUBJECT, of(ENGLISH.getLanguage(), "otherSubject"),
                FROM, of(ENGLISH.getLanguage(), "otherFrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoFieldReturnDefault() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigHasTranslationKeyReturnTranslationByKey() {
        when(localizationMessageService.getMessage("tr subject key")).thenReturn("subject value");
        when(localizationMessageService.getMessage("tr from key")).thenReturn("fromvalue (From value caption)");

        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(TRANSLATION_KEY, "tr subject key", ENGLISH.getLanguage(), "en subject", FRANCE.getLanguage(), "fr subject"),
                FROM, of(TRANSLATION_KEY, "tr from key", ENGLISH.getLanguage(), "en from", FRANCE.getLanguage(), "frfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("fromvalue (From value caption)")[0]));
        verify(mock).setSubject(eq("subject value"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTranslationKeyReturnByLocale() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(ENGLISH.getLanguage(), "en subject", FRANCE.getLanguage(), "fr subject"),
                FROM, of(ENGLISH.getLanguage(), "en from", FRANCE.getLanguage(), "frfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("frfrom")[0]));
        verify(mock).setSubject(eq("fr subject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTranslationKeyAndNoTranslationsByLocaleReturnEn() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(ENGLISH.getLanguage(), "en subject"),
                FROM, of(ENGLISH.getLanguage(), "enfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("enfrom")[0]));
        verify(mock).setSubject(eq("en subject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testSubjectConfiguration() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                SUBJECT, of(FRANCE.getLanguage(), "otherSubject")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq("otherSubject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testFromConfiguration() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                FROM, of(FRANCE.getLanguage(), "otherFrom@yopmail.com (France caption)")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("otherFrom@yopmail.com (France caption)")[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testSubjectAndFromConfiguration() {
        when(tenantConfigService.getConfig()).thenReturn(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                SUBJECT, of(FRANCE.getLanguage(), "otherSubject"),
                FROM, of(FRANCE.getLanguage(), "otherFrom@yopmail.com (France caption)")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("otherFrom@yopmail.com (France caption)")[0]));
        verify(mock).setSubject(eq("otherSubject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

}
