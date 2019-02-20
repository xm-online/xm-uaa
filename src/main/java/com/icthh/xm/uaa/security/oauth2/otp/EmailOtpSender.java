package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link EmailOtpSender} class.
 */
public class EmailOtpSender implements OtpSender {

    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
    private final MailService mailService;
    private final UserService userService;

    public EmailOtpSender(TenantContextHolder tenantContextHolder,
                          XmRequestContextHolder xmRequestContextHolder,
                          MailService mailService,
                          UserService userService) {
        this.tenantContextHolder = tenantContextHolder;
        this.xmRequestContextHolder = xmRequestContextHolder;
        this.mailService = mailService;
        this.userService = userService;
    }

    @Override
    public void send(String otp, String destination, String userKey) {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);

        User user = userService.getUser(userKey);
        if (user == null) {
            throw new IllegalStateException("User with key '" + userKey + "' not found in tenant: " + tenantKey.getValue());
        }

        String applicationUrl = UaaUtils.getApplicationUrl(xmRequestContextHolder);

        Map<String, Object> dataBind = new HashMap<>();
        dataBind.put("otp", otp);
        dataBind.put("user", user);
        dataBind.put("tenant", tenantKey.getValue());
        dataBind.put("appBaseUrl", applicationUrl);

        mailService.sendEmailFromTemplate(tenantKey,
                                          user,
                                          "tfaOtpEmail",
                                          "email.tfa.otp.title",
                                          destination,
                                          dataBind,
                                          MdcUtils.getRid());
    }

}
