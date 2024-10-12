package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link EmailOtpSender} class.
 */
@Component
public class EmailOtpSender implements OtpSender {

    public static final String AUTH_OTP_MESSAGE_TYPE = "email-auth-otp";

    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
    private final MailService mailService;
    private final UserService userService;
    private final TenantPropertiesService tenantPropertiesService;

    public EmailOtpSender(TenantContextHolder tenantContextHolder,
                          XmRequestContextHolder xmRequestContextHolder,
                          MailService mailService,
                          UserService userService,
                          TenantPropertiesService tenantPropertiesService) {
        this.tenantContextHolder = tenantContextHolder;
        this.xmRequestContextHolder = xmRequestContextHolder;
        this.mailService = mailService;
        this.userService = userService;
        this.tenantPropertiesService = tenantPropertiesService;
    }

    @Override
    public void send(OtpSendDTO otpSendDTO) {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);

        String userKey = otpSendDTO.getUserKey();
        User user = userService.getUser(userKey);
        if (user == null) {
            throw new IllegalStateException("User with key '" + userKey + "' not found in tenant: " + tenantKey.getValue());
        }

        String applicationUrl = UaaUtils.getApplicationUrl(xmRequestContextHolder);

        Map<String, Object> dataBind = new HashMap<>();
        dataBind.put("otp", otpSendDTO.getOtp());
        dataBind.put("user", user);
        dataBind.put("tenant", tenantKey.getValue());
        dataBind.put("appBaseUrl", applicationUrl);

        mailService.sendEmailFromTemplate(tenantKey,
                                          user,
                                          getTemplateName(otpSendDTO),
                                          getTitleKey(otpSendDTO),
                                          otpSendDTO.getDestination(),
                                          dataBind,
                                          MdcUtils.getRid());
    }

    private String getTemplateName(OtpSendDTO otpSendDTO) {
        if (otpSendDTO.isNotEmptyTemplateName()) {
            return otpSendDTO.getTemplateName();
        }
        return Optional.ofNullable(tenantPropertiesService.getTenantProps(getTenantKey()))
            .map(p -> p.getCommunication().getNotifications().get(otpSendDTO.getNotificationKey()).getTemplateName())
            .orElseThrow(() -> new BusinessException(
                "Message notification configuration is missing by key: " + otpSendDTO.getNotificationKey()));
    }

    private String getTitleKey(OtpSendDTO otpSendDTO) {
        if (otpSendDTO.isNotEmptyTitleKey()) {
            return otpSendDTO.getTitleKey();
        }
        return Optional.ofNullable(tenantPropertiesService.getTenantProps(getTenantKey()))
            .map(p -> p.getCommunication().getNotifications().get(otpSendDTO.getNotificationKey()).getTitleKey())
            .orElseThrow(() -> new BusinessException(
                "Message notification configuration is missing by key: " + otpSendDTO.getNotificationKey()));
    }

    private TenantKey getTenantKey() {
        return TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
    }
}
