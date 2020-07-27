package com.icthh.xm.uaa.service.account.password.reset.type.email;

import com.icthh.xm.uaa.service.AccountMailService;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetHandler;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetRequest;
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
