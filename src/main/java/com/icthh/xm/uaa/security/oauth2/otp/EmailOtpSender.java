package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link EmailOtpSender} class.
 */
@Component
public class EmailOtpSender extends AbstractOtpSender {

    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
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
        this.xmRequestContextHolder = xmRequestContextHolder;
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

        String applicationUrl = UaaUtils.getApplicationUrl(xmRequestContextHolder);

        Map<String, Object> dataBind = new HashMap<>();
        dataBind.put("otp", otpSendDTO.getOtp());
        dataBind.put("user", new UserDTO(user));
        dataBind.put("tenant", tenantKey.getValue());
        dataBind.put("appBaseUrl", applicationUrl);

        mailService.sendEmailFromTemplate(tenantKey,
                                          user,
                                          otpSendDTO.getChannel().getTemplateName(),
                                          otpSendDTO.getChannel().getTitleKey(),
                                          otpSendDTO.getDestination(),
                                          dataBind,
                                          MdcUtils.getRid());
    }
}
