package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@LepService(group = "service.account")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountMailService {

    private final TenantContextHolder tenantContextHolder;
    private final ApplicationUrlService applicationUrlService;
    private final MailService mailService;

    @LogicExtensionPoint("SendMailOnRegistration")
    public void sendMailOnRegistration(User user) {
        mailService.sendActivationEmail(user,
            applicationUrlService.getApplicationUrl(),
            TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
            MdcUtils.getRid());
    }

    @LogicExtensionPoint("SendMailOnPasswordReset")
    public void sendMailOnPasswordResetFinish(User user) {
        mailService.sendPasswordChangedMail(user,
            applicationUrlService.getApplicationUrl(),
            TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
            MdcUtils.getRid());
    }

    @LogicExtensionPoint("SendMailOnPasswordInit")
    public void sendMailOnPasswordInit(User user) {
        mailService.sendPasswordResetMail(user,
            applicationUrlService.getApplicationUrl(),
            TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
            MdcUtils.getRid());
    }

}
