package com.icthh.xm.uaa.web.rest.vm;

import lombok.Data;

@Data
public class ResetPasswordVM {
    String mail;
    String login;
    String resetType;
    String loginType;
}
