package com.icthh.xm.uaa.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import freemarker.template.Configuration;
import io.github.jhipster.config.JHipsterProperties;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.icthh.xm.uaa.service.mail.MailService.ACTIVATION_EMAIL_TEMPLATE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class MailServiceIntTest {

    private static final TenantKey TEST_TENANT_KEY = TenantKey.valueOf("test");
    private static final String EMAIL_FROM = "test@xm-online.com";
    private static final String EMAIL_TO = "john.doe@example.com";
    private static final String EMAIL_SUFFIX = "@xm-online.com";
    private static final String APPLICATION_URL = "http://xm.local:8080";
    private static final String DEFAULT_FIRST_NAME = "AAAAAAAAA";
    private static final String DEFAULT_LANG_KEY = "en";
    private static final String TEMPLATE_NAME = "testTemplate";
    private static final String EMAIL_TYPE = "Email";

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

    @Mock
    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private LocalizationMessageService localizationMessageService;

    @MockBean
    private CommunicationService communicationService;

    private JavaMailSenderImpl javaMailSender = spy(JavaMailSenderImpl.class);

    @Spy
    private MailProviderService mailProviderService = new MailProviderService(javaMailSender);

    @Captor
    private ArgumentCaptor messageCaptor;

    @InjectMocks
    private MailService mailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Spy
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.Communication appCommunication;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, TEST_TENANT_KEY);

        tenantEmailTemplateService
            .onRefresh("/config/tenants/" + TEST_TENANT_KEY.getValue() + "/uaa/emails/"
                           + DEFAULT_LANG_KEY + "/testTemplate.ftl",
                       "Hello, <#if (user.firstName)??>AAAAAAAAA<#else>testEmptyVariable</#if>! You've got a letter from ${tenant}");

        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        mailService = new MailService(jHipsterProperties, mailProviderService, messageSource,
                                      tenantEmailTemplateService, freeMarker, tenantContextHolder,
                                      tenantConfigService, localizationMessageService, communicationService,
                                      objectMapper, applicationProperties);
    }

    @After
    public void destroy() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testSendEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com",
                              "testSubject",
                              "testContent",
                              TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX,
                              javaMailSender);
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
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", "test@xm-online.com",
                              javaMailSender);
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
        verifyZeroInteractions(communicationService);
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
    public void testSendEmailWithException() {
        doThrow(MailSendException.class).when(javaMailSender).send(any(MimeMessage.class));
        mailService.sendEmail("john.doe@example.com",
                              "testSubject",
                              "testContent",
                              TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX,
                              javaMailSender);
    }

    @Test
    public void testSendEmailFromTemplateByCommunicationWithSystemEmail() {
        String from = TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX;
        User user = new User();
        user.setLangKey(DEFAULT_LANG_KEY);
        user.setAuthorities(List.of("ROLE_USER"));
        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model.put("tenant", TEST_TENANT_KEY.getValue());
        String convertedModel = convertToString(model);

        when(applicationProperties.getCommunication()).thenReturn(appCommunication);
        when(appCommunication.isEnabled()).thenReturn(TRUE);

        mailService.sendEmailFromTemplate(TEST_TENANT_KEY,
            user,
            ACTIVATION_EMAIL_TEMPLATE,
            "email.activation.title",
            "john.doe@example.com",
            model);

        verify(communicationService).sendEmailEvent(isCorrectMessage(from, convertedModel, null, ACTIVATION_EMAIL_TEMPLATE));
        verifyNoMoreInteractions(communicationService);
    }

    @Test
    public void testSendEmailFromTemplateByCommunicationWithNotSystemEmail() {
        String from = TEST_TENANT_KEY.getValue() + EMAIL_SUFFIX;
        String subject = "test title";
        User user = new User();
        user.setLangKey(DEFAULT_LANG_KEY);
        user.setAuthorities(List.of("ROLE_USER"));
        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model.put("tenant", TEST_TENANT_KEY.getValue());
        String convertedModel = convertToString(model);

        when(applicationProperties.getCommunication()).thenReturn(appCommunication);
        when(appCommunication.isEnabled()).thenReturn(TRUE);

        mailService.sendEmailFromTemplate(TEST_TENANT_KEY,
            user,
            TEMPLATE_NAME,
            "email.test.title",
            "john.doe@example.com",
            model);

        verify(communicationService).sendEmailEvent(isCorrectMessage(from, convertedModel, subject, TEMPLATE_NAME));
        verifyNoMoreInteractions(communicationService);
    }

    private CommunicationMessage isCorrectMessage(String sender, String model, String subject, String templateName) {
        return argThat((CommunicationMessage message) -> EMAIL_TO.equals(message.getReceiver().get(0).getEmail())
        && sender.equals(message.getSender().getId())
        && DEFAULT_LANG_KEY.equals(message.getCharacteristic().get(0).getValue())
        && templateName.equals(message.getCharacteristic().get(1).getValue())
        && model.equals(message.getCharacteristic().get(2).getValue())
        && EMAIL_TYPE.equals(message.getType())
        && Objects.equals(message.getSubject(), subject));
    }

    @SneakyThrows
    private String convertToString(Map object){
        return objectMapper.writeValueAsString(object);
    }
}
