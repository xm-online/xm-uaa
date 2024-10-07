package com.icthh.xm.uaa.web.rest.vm;

import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.Getter;
import lombok.Setter;

public class AuthorizeUserVm extends UserDTO {

    @Setter
    @Getter
    private String captcha;
}
