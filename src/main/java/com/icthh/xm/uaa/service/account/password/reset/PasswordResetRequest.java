package com.icthh.xm.uaa.service.account.password.reset;

import com.icthh.xm.uaa.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PasswordResetRequest {
    private String resetType;
    private User user;
}
