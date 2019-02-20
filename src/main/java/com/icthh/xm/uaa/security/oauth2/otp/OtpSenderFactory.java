package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.domain.OtpChannelType;

import java.util.Optional;

/**
 * The {@link OtpSenderFactory} interface.
 */
public interface OtpSenderFactory {

    Optional<OtpSender> getSender(OtpChannelType channelType);

}
