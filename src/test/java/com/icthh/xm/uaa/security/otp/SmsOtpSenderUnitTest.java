package com.icthh.xm.uaa.security.otp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.oauth2.otp.SmsOtpSender;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.messaging.UserMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SmsOtpSenderUnitTest {

    private static final TenantKey TENANT_KEY = TenantKey.valueOf("XM");

    private static final String OTP_CODE = "123456";
    private static final String USERNAME = "user@test.com.ua";
    private static final String USER_KEY = UUID.randomUUID().toString();

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Mock
    private TenantPropertiesService tenantPropertiesService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private UserMessagingService userMessagingService;
    @Mock
    private CommunicationService communicationService;

    @Mock
    private UserService userService;
    @Mock
    private XmRequestContextHolder xmRequestContextHolder;

    private SmsOtpSender smsOtpSender;

    private OtpSendDTO otpSendDTO;
    private User mockUser;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TENANT_KEY));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        XmRequestContext xmRequestContext = mock(XmRequestContext.class);
        when(xmRequestContextHolder.getContext()).thenReturn(xmRequestContext);

        TenantProperties tenantProperties = buildTenantProperties();
        when(tenantPropertiesService.getTenantProps(eq(TENANT_KEY))).thenReturn(tenantProperties);

        TenantProperties.NotificationChannel channel = tenantProperties.getCommunication()
            .getNotifications().get("auth-otp")
            .getChannels().get("sms");

        otpSendDTO = new OtpSendDTO(OTP_CODE, USERNAME, USER_KEY, channel);

        mockUser = mock(User.class);
        when(mockUser.getLangKey()).thenReturn("ua");

        smsOtpSender = new SmsOtpSender(tenantPropertiesService, applicationProperties, tenantContextHolder,
            userMessagingService, communicationService, objectMapper, userService, xmRequestContextHolder);
    }

    @Test
    public void test_communicationEnabled_shouldSendEventWithCommunication() {
        when(userService.getUser(USER_KEY)).thenReturn(mockUser);

        smsOtpSender.send(otpSendDTO);

        verify(communicationService, times(1)).sendEmailEvent(any(CommunicationMessage.class));
        verify(userMessagingService, never()).sendAuthorizeMessage(any(OtpSendDTO.class));
    }

    @Test
    public void test_communicationDisabled_shouldSendEventWithUserMessagingService() {
        TenantProperties tenantProperties = buildTenantProperties();
        tenantProperties.getCommunication().setEnabled(false);
        when(tenantPropertiesService.getTenantProps(eq(TENANT_KEY))).thenReturn(tenantProperties);
        when(userService.getUser(USER_KEY)).thenReturn(mockUser);

        smsOtpSender.send(otpSendDTO);

        verify(communicationService, never()).sendEmailEvent(any(CommunicationMessage.class));
        verify(userMessagingService, times(1)).sendAuthorizeMessage(any(OtpSendDTO.class));
    }

    @Test
    public void test_userNotFound_shouldThrowIllegalStateException() {
        when(userService.getUser(USER_KEY)).thenReturn(null);

        assertThrows(String.format("User by key '%s' not found in tenant: %s", USER_KEY, TENANT_KEY.getValue()),
            IllegalStateException.class, () -> smsOtpSender.send(otpSendDTO)
        );
    }

    @Test
    void test_communicationEnabled_shouldCreateCorrectCommunicationMessage() {
        when(userService.getUser(USER_KEY)).thenReturn(mockUser);

        ArgumentCaptor<CommunicationMessage> messageCaptor = ArgumentCaptor.forClass(CommunicationMessage.class);

        smsOtpSender.send(otpSendDTO);

        // Verify communication message
        verify(communicationService).sendEmailEvent(messageCaptor.capture());
        CommunicationMessage capturedMessage = messageCaptor.getValue();

        assertEquals(otpSendDTO.getChannel().getType(), capturedMessage.getType());
        assertEquals(otpSendDTO.getDestination(), capturedMessage.getReceiver().get(0).getPhoneNumber());
        assertEquals(otpSendDTO.getChannel().getKey(), capturedMessage.getSender().getId());

        Map<String, String> resultMessageCharacteristic = capturedMessage.getCharacteristic().stream()
            .collect(Collectors.toMap(c -> c.getName(), c -> c.getValue()));

        assertEquals(otpSendDTO.getChannel().getTemplateName(), resultMessageCharacteristic.get("templateName"));
        assertEquals(mockUser.getLangKey(), resultMessageCharacteristic.get("language"));

        Map<String, Object> resultTemplateModel = getTemplateModelMap(resultMessageCharacteristic.get("templateModel"));

        assertEquals(OTP_CODE, resultTemplateModel.get("otp"));
        assertEquals(TENANT_KEY.getValue(), resultTemplateModel.get("tenant"));
        assertNotNull(resultTemplateModel.get("appBaseUrl"));
        assertNotNull(resultTemplateModel.get("user"));
    }

    private Map<String, Object> getTemplateModelMap(String config) {
        try {
            return objectMapper.readValue(config, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error when read template mode map: " + e.getMessage());
        }
    }

    private TenantProperties buildTenantProperties() {
        TenantProperties.NotificationChannel channel1 = new TenantProperties.NotificationChannel();
        channel1.setKey("SMS_NOTIFICATION");
        channel1.setType("Twilio");
        channel1.setTemplateName("sms.template.name");

        TenantProperties.NotificationChannel channel2 = new TenantProperties.NotificationChannel();
        channel2.setKey("EMAIL_NOTIFICATION");
        channel2.setType("TemplatedEmail");
        channel2.setTemplateName("email.template.name");

        TenantProperties.Notification notification = new TenantProperties.Notification();
        notification.setChannels(Map.of("sms", channel1, "email", channel2));

        TenantProperties.Communication communication = new TenantProperties.Communication();
        communication.setEnabled(true);
        communication.setNotifications(Map.of("auth-otp", notification));

        TenantProperties properties = new TenantProperties();
        properties.setCommunication(communication);
        return properties;
    }
}
