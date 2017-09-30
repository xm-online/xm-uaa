package com.icthh.xm.uaa.web.rest.vm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVM {
    private Boolean isCaptchaNeed;
    private String publicKey;
}
