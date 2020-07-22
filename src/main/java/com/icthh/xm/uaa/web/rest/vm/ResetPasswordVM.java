package com.icthh.xm.uaa.web.rest.vm;

import lombok.Data;

@Data
public class ResetPasswordVM {
    String login;
    String resetType;
    String loginType;
}
