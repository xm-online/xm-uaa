package com.icthh.xm.uaa.service.mail;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import freemarker.template.Configuration;
import io.github.jhipster.config.JHipsterProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class MailServiceIntTest {

    private static final String DEFAULT_DOMAIN = "xm.local";
    private static final TenantKey TEST_TENANT_KEY = TenantKey.valueOf("test");
    private static final String EMAIL_FROM = "test@xm-online.com";
    private static final String EMAIL_SUFFIX = "@xm-online.com";
    private static final String APPLICATION_URL = "http://xm.local:8080";
    private static final String DEFAULT_FIRST_NAME = "AAAAAAAAA";
    private static final String DEFAULT_LANG_KEY = "en";

    @Autowired
    private JHipsterProperties jHipsterProperties;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private Configuration freeMarker;

    @Autowired
    private TenantEmailTemplateService tenantEmailTemplateService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private LocalizationMessageService localizationMessageService;

    @Spy
    private JavaMailSenderImpl javaMailSender;

    @Captor
    private ArgumentCaptor messageCaptor;

    private MailService mailService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, TEST_TENANT_KEY);

        tenantEmailTemplateService
            .onRefresh("/config/tenants/" + TEST_TENANT_KEY.getValue() + "/uaa/emails/"
                           + DEFAULT_LANG_KEY + "/testTemplate.ftl",
                       "Hello, <#if (user.firstName)??>AAAAAAAAA<#else>testEmptyVariable</#if>! You've got a letter from ${tenant}");


        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        mailService = new MailService(jHipsterProperties, javaMailSender, messageSource,
                                      tenantEmailTemplateService, freeMarker, tenantContextHolder,
                                      tenantConfigService, localizationMessageService);
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testSendEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("john.doe@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo(EMAIL_FROM);
        assertThat(message.getContent()).isInstanceOf(String.class);
        assertThat(message.getContent().toString()).isEqualTo("testContent");
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testSendHtmlEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", "test@xm-online.com");
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("john.doe@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo(EMAIL_FROM);
        assertThat(message.getFrom()[0].toString()).isEqualTo(TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        assertThat(message.getContent()).isInstanceOf(String.class);
        assertThat(message.getContent().toString()).isEqualTo("testContent");
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testSendEmailFromTemplate() throws Exception {
        User user = new User();
        user.setLangKey("en");
        Map<String, Object> objectModel = new HashMap<>();
        objectModel.put("user", user);
        objectModel.put("tenant", TEST_TENANT_KEY.getValue());
        mailService.sendEmailFromTemplate(TEST_TENANT_KEY,
                                          user,
                                          "testTemplate",
                                          "email.test.title",
                                          "john.doe@example.com",
                                          objectModel);
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("test title");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("john.doe@example.com");
        assertThat(message.getFrom()[0].toString()).isEqualTo(TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        assertThat(message.getContent().toString()).isEqualTo(
            "Hello, testEmptyVariable! You've got a letter from " + TEST_TENANT_KEY.getValue());
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testSendActivationEmail() throws Exception {
        tenantEmailTemplateService.onRefresh("/config/tenants/" + TEST_TENANT_KEY.getValue() + "/uaa/emails/"
                                                 + DEFAULT_LANG_KEY + "/activationEmail.ftl",
                                             "Hello, ${user.firstName}! You've got a letter from ${tenant}");

        User user = new User();
        user.setFirstName(DEFAULT_FIRST_NAME);
        user.setLangKey("en");
        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("john.doe@example.com");
        user.getLogins().add(userLogin);
        mailService.sendActivationEmail(user, APPLICATION_URL, TEST_TENANT_KEY, MdcUtils.getRid());
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(user.getEmail());
        assertThat(message.getFrom()[0].toString()).isEqualTo(TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        assertThat(message.getContent().toString()).isNotEmpty();
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testCreationEmail() throws Exception {
        tenantEmailTemplateService.onRefresh("/config/tenants/" + TEST_TENANT_KEY.getValue() + "/uaa/emails/"
                                                 + DEFAULT_LANG_KEY + "/creationEmail.ftl",
                                             "Hello, ${user.firstName}! You've got a letter from ${tenant}");

        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("john.doe@example.com");

        User user = new User();
        user.setFirstName("john");
        user.setUserKey("test");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        mailService.sendCreationEmail(user, APPLICATION_URL, TEST_TENANT_KEY, MdcUtils.getRid());
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(user.getEmail());
        assertThat(message.getFrom()[0].toString()).isEqualTo(TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        assertThat(message.getContent().toString()).isNotEmpty();
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testSendPasswordResetMail() throws Exception {
        tenantEmailTemplateService.onRefresh("/config/tenants/" + TEST_TENANT_KEY.getValue() + "/uaa/emails/"
                                                 + DEFAULT_LANG_KEY + "/passwordResetEmail.ftl",
                                             "Hello, ${user.firstName}! You've got a letter from ${tenant}");

        UserLogin userLogin = new UserLogin();
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin("john.doe@example.com");

        User user = new User();
        user.setFirstName(DEFAULT_FIRST_NAME);
        user.setUserKey("test");
        user.setLangKey(DEFAULT_LANG_KEY);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.getLogins().add(userLogin);
        userLogin.setUser(user);

        mailService.sendPasswordResetMail(user, APPLICATION_URL, TEST_TENANT_KEY, null);
        verify(javaMailSender).send((MimeMessage) messageCaptor.capture());
        MimeMessage message = (MimeMessage) messageCaptor.getValue();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(user.getEmail());
        assertThat(message.getFrom()[0].toString()).isEqualTo(TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
        assertThat(message.getContent().toString()).isNotEmpty();
        assertThat(message.getDataHandler().getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    public void testSendEmailWithException() throws Exception {
        doThrow(MailSendException.class).when(javaMailSender).send(any(MimeMessage.class));
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX);
    }

}
