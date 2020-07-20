package com.icthh.xm.uaa.service.user.password;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@LepService(group = "service.password.reset")
@Service
@Slf4j
public class CustomPasswordResetHandler implements PasswordResetHandler {

    @Override
    @LogicExtensionPoint(value = "ResetPassword", resolver = CustomPasswordResetHandlerResolver.class)
    public void handle(PasswordResetRequest resetRequest) {
        log.warn("No lep for type: {}", resetRequest.getResetType());
    }
}
