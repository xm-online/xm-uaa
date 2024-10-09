package com.icthh.xm.uaa.web.rest.vm;

import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

public class AuthorizeUserVm extends UserDTO {

    @NotEmpty
    @Getter
    private String login;

    @Setter
    @Getter
    private String captcha;

    public AuthorizeUserVm(String login, String captcha) {
        this.captcha = captcha;
        this.login = login;
        addUserLogin(login);
    }
}
