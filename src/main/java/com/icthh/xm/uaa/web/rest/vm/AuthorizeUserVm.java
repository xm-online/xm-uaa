package com.icthh.xm.uaa.web.rest.vm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizeUserVm {

    @NotEmpty
    private String login;

    @NotEmpty
    private String langKey;

    private String captcha;
}
