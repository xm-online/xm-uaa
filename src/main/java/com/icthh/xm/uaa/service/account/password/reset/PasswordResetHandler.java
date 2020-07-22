package com.icthh.xm.uaa.service.account.password.reset;

import com.icthh.xm.uaa.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface PasswordResetHandler {
    void handle(PasswordResetRequest resetRequest);

    @AllArgsConstructor
    @Getter
    class PasswordResetRequest {
        private String resetType;
        private User user;
    }
}
