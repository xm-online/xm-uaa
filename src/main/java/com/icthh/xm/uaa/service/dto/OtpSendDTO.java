package com.icthh.xm.uaa.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OtpSendDTO {

    private String otp;
    private String destination;
    private String userKey;
    private String templateName;
    private String titleKey;

}
