package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class OtpSendDTO {

    private String otp;
    private String destination;
    private String userKey;
    @Setter(AccessLevel.NONE)
    private TenantProperties.NotificationChannel channel;
    // fields to support old tfa otp logic
    private String titleKey;
    private String templateName;

    public OtpSendDTO(String otp, String destination, String userKey, TenantProperties.NotificationChannel channel) {
        this.otp = otp;
        this.destination = destination;
        this.userKey = userKey;
        this.channel = channel;
    }

    public OtpSendDTO(String otp, String destination, String userKey, String templateName, String titleKey) {
        this.otp = otp;
        this.destination = destination;
        this.userKey = userKey;
        this.templateName = templateName;
        this.titleKey = titleKey;
    }
}
