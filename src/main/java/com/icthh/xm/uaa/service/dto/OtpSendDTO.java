package com.icthh.xm.uaa.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class OtpSendDTO {

    private String otp;
    private String destination;
    private String userKey;
    private String templateName;
    private String titleKey;
    private String notificationKey; // configuration key in uaa.yml to get notification template data

    public OtpSendDTO(String otp, String destination, String userKey, String templateName, String titleKey) {
        this.otp = otp;
        this.destination = destination;
        this.templateName = templateName;
        this.userKey = userKey;
        this.titleKey = titleKey;
    }

    public OtpSendDTO(String otp, String destination, String userKey, String notificationKey) {
        this.otp = otp;
        this.destination = destination;
        this.userKey = userKey;
        this.notificationKey = notificationKey;
    }

    public boolean isNotEmptyTemplateName() {
        return StringUtils.isNotEmpty(templateName);
    }

    public boolean isNotEmptyTitleKey() {
        return StringUtils.isNotEmpty(templateName);
    }
}
