package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.service.dto.OtpSendDTO;

/**
 * The {@link OtpSender} interface.
 */
public interface OtpSender {

    void send(OtpSendDTO otpSendDTO);

}
