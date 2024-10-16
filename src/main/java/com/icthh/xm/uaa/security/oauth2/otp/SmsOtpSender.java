package com.icthh.xm.uaa.security.oauth2.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.messaging.communication.CommunicationMessage;
import com.icthh.xm.commons.messaging.communication.CommunicationMessageBuilder;
import com.icthh.xm.commons.messaging.communication.Receiver;
import com.icthh.xm.commons.messaging.communication.service.CommunicationService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.messaging.UserMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
public class SmsOtpSender extends AbstractOtpSender {

    private final UserMessagingService userMessagingService;
    private final CommunicationService communicationService;
    private final ObjectMapper objectMapper;

    public SmsOtpSender(TenantPropertiesService tenantPropsService,
                        ApplicationProperties applicationProps,
                        TenantContextHolder tenantContextHolder,
                        UserMessagingService userMessagingService,
                        CommunicationService communicationService,
                        ObjectMapper objectMapper,
                        UserService userService,
                        XmRequestContextHolder xmContextHolder) {
        super(tenantPropsService, applicationProps, tenantContextHolder, userService, xmContextHolder);
        this.userMessagingService = userMessagingService;
        this.communicationService = communicationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(OtpSendDTO otpSendDTO) {
        TenantKey tenantKey = getTenantKey();
        if (isCommunicationEnabled(tenantKey)) {
            sendCommunicationEvent(tenantKey, otpSendDTO);
        } else {
            userMessagingService.sendAuthorizeMessage(otpSendDTO);
        }
    }

    private void sendCommunicationEvent(TenantKey tenantKey, OtpSendDTO otpSendDTO) {
        User user = getUserByUserKey(otpSendDTO.getUserKey(), tenantKey.getValue());
        CommunicationMessage communicationMessage = getCommunicationMessage(tenantKey, otpSendDTO, user);
        communicationService.sendEmailEvent(communicationMessage);
    }

    private CommunicationMessage getCommunicationMessage(TenantKey tenantKey, OtpSendDTO otpSendDTO, User user) {
        CommunicationMessage message = new CommunicationMessage();
        message.setCharacteristic(new ArrayList<>());
        message.setType(otpSendDTO.getChannel().getType());
        message.getReceiver().add(getReceiver(otpSendDTO.getDestination()));

        message = new CommunicationMessageBuilder(message, objectMapper)
            .addSenderId(otpSendDTO.getChannel().getKey())
            .addLanguage(user.getLangKey())
            .addTemplateName(otpSendDTO.getChannel().getTemplateName())
            .addTemplateModel(getObjectModel(otpSendDTO.getOtp(), user, tenantKey))
            .build();
        return message;
    }

    private Receiver getReceiver(String phoneNumber) {
        Receiver receiver = new Receiver();
        receiver.setPhoneNumber(phoneNumber);
        return receiver;
    }
}
