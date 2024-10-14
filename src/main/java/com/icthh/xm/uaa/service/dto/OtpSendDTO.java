package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
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
    private TenantProperties.NotificationChannel channel;

    public OtpSendDTO(String otp, String destination, String userKey) {
        this.otp = otp;
        this.destination = destination;
        this.userKey = userKey;
    }

    public void setTitleKey(String titleKey) {
        if (channel == null) {
            channel = new TenantProperties.NotificationChannel();
        }
        channel.setTitleKey(titleKey);
    }

    public void setTemplateName(String templateName) {
        if (channel == null) {
            channel = new TenantProperties.NotificationChannel();
        }
        channel.setTemplateName(templateName);
    }
}
