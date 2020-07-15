package com.icthh.xm.uaa.service.user.password.reset.email;

import com.icthh.xm.uaa.service.AccountMailService;
import com.icthh.xm.uaa.service.user.password.PasswordResetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPasswordResetHandler implements PasswordResetHandler {

    private final AccountMailService accountMailService;

    @Override
    public void handle(PasswordResetRequest resetRequest) {
        accountMailService.sendMailOnPasswordInit(resetRequest.getUser());
    }
}
