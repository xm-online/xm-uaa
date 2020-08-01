package com.icthh.xm.uaa.service.account.password.reset.type.custom;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetHandler;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@LepService(group = "service.user")
@Service
@Slf4j
public class CustomPasswordResetHandler implements PasswordResetHandler {

    @Override
    @LogicExtensionPoint(value = "SendPasswordResetKey", resolver = CustomPasswordResetHandlerResolver.class)
    public void handle(PasswordResetRequest resetRequest) {
        log.warn("No lep for type: {}", resetRequest.getResetType());
    }
}
