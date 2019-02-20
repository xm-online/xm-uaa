package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.uaa.domain.OtpChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * The {@link DefaultOtpSenderFactory} class.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOtpSenderFactory implements OtpSenderFactory {

    private final EmailOtpSender emailOtpSender;

    @Override
    public Optional<OtpSender> getSender(OtpChannelType channelType) {
        switch (channelType) {
            case EMAIL:
                return Optional.of(emailOtpSender);

            default:
                log.warn("OTP sender factory has no configured sender for channel type {}", channelType);
                return Optional.empty();
        }
    }

}
