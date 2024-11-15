package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.stereotype.Component;

/**
 * The {@link EmailOtpSender} class.
 */
@Component
public class EmailOtpSender extends AbstractOtpSender {

    private final TenantContextHolder tenantContextHolder;
    private final MailService mailService;
    private final UserService userService;

    public EmailOtpSender(TenantPropertiesService tenantPropsService,
                          ApplicationProperties applicationProps,
                          TenantContextHolder tenantContextHolder,
                          XmRequestContextHolder xmRequestContextHolder,
                          MailService mailService,
                          UserService userService) {
        super(tenantPropsService, applicationProps, tenantContextHolder, userService, xmRequestContextHolder);
        this.tenantContextHolder = tenantContextHolder;
        this.mailService = mailService;
        this.userService = userService;
    }

    @Override
    public void send(OtpSendDTO otpSendDTO) {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);

        String userKey = otpSendDTO.getUserKey();
        User user = userService.getUser(userKey);
        if (user == null) {
            throw new IllegalStateException("User with key '" + userKey + "' not found in tenant: " + tenantKey.getValue());
        }

        mailService.sendEmailFromTemplate(tenantKey,
                                          user,
                                          otpSendDTO.getTemplateName(),
                                          otpSendDTO.getTitleKey(),
                                          otpSendDTO.getDestination(),
                                          getObjectModel(otpSendDTO.getOtp(), user, tenantKey),
                                          MdcUtils.getRid());
    }
}
