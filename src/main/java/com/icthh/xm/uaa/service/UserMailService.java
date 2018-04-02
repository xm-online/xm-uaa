package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@LepService(group = "service.user")
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserMailService {

    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
    private final MailService mailService;

    @LogicExtensionPoint("SendMailOnCreateUser")
    public void sendMailOnCreateUser(User newUser) {
        mailService.sendCreationEmail(newUser,
                                      UaaUtils.getApplicationUrl(xmRequestContextHolder),
                                      TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
                                      MdcUtils.getRid());
    }
}
