package com.icthh.xm.uaa.service.account.password.reset;

public interface PasswordResetHandler {
    void handle(PasswordResetRequest resetRequest);
}
