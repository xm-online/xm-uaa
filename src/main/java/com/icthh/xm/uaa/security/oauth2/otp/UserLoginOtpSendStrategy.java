package com.icthh.xm.uaa.security.oauth2.otp;

import static com.icthh.xm.uaa.config.Constants.REQ_ATTR_TFA_OTP_CHANNEL_TYPE;

import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import com.icthh.xm.uaa.util.OtpUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link UserLoginOtpSendStrategy} class.
 */
public class UserLoginOtpSendStrategy implements OtpSendStrategy {

    private final TenantPropertiesService tenantPropertiesService;
    private final OtpSenderFactory otpSenderFactory;

    public UserLoginOtpSendStrategy(TenantPropertiesService tenantPropertiesService, OtpSenderFactory otpSenderFactory) {
        this.tenantPropertiesService = Objects.requireNonNull(tenantPropertiesService, "tenantPropertiesService can't be null");
        this.otpSenderFactory = Objects.requireNonNull(otpSenderFactory, "otpSenderFactory can't be null");
    }

    @Override
    public void send(String otp, DomainUserDetails userDetails) {
        OtpChannel otpChannel = getOtpChannel(userDetails);
        OtpSender sender = otpSenderFactory.getSender(otpChannel.getType()).orElseThrow(
            () -> new AuthenticationServiceException("Can't find OTP sender for channel type: "
                                                         + otpChannel.getType().getTypeName())
        );

        // Enrich user details with OTP channel type (User additional details will be copied to JWT access token)
        userDetails.setTfaOtpChannelType(otpChannel.getType());
        // Enrich request context with OTP channel type (it will be copied to response HTTP header)
        RequestContextHolder.getRequestAttributes().setAttribute(REQ_ATTR_TFA_OTP_CHANNEL_TYPE,
                                                                 otpChannel.getType(),
                                                                 RequestAttributes.SCOPE_REQUEST);

        sender.send(otp, otpChannel.getDestination(), userDetails.getUserKey());
    }

    private OtpChannel getOtpChannel(DomainUserDetails userDetails) {
        final OtpChannelType channelType = getNotNullOtpChannelType(userDetails);

        // find first appropriate UserLogin for OTP channel type
        Optional<UserLoginDto> userLogin = userDetails.getLogins().stream().filter(userLoginDto -> {
            UserLoginType loginType = OtpUtils.getRequiredLoginType(userLoginDto.getTypeKey());
            Optional<List<OtpChannelType>> supportedOtpChannelTypes = OtpUtils.getSupportedOtpChannelTypes(loginType);

            return supportedOtpChannelTypes.map(otpChannelTypes -> otpChannelTypes.contains(channelType)).orElse(false);
        }).findFirst();

        Optional<OtpChannel> channel = userLogin.map(userLoginDto -> new OtpChannel(channelType, userLoginDto.getLogin()));
        return channel.orElseThrow(
            () -> new InternalAuthenticationServiceException("User (key: " + userDetails.getUserKey()
                                                                 + ", tenant: " + userDetails.getTenant()
                                                                 + ") has no appropriate channel for type: "
                                                                 + channelType.getTypeName())
        );
    }

    private OtpChannelType getNotNullOtpChannelType(DomainUserDetails userDetails) {
        // get user OTP channel type
        final Optional<OtpChannelType> userChannelType = userDetails.getTfaOtpChannelType();

        Optional<OtpChannelType> channelTypeOptional;
        if (userChannelType.isPresent()) {
            channelTypeOptional = userChannelType;
        } else {
            // get tenant OTP channel type
            String tenantOtpChannelTypeStr = tenantPropertiesService.getTenantProps().getSecurity().getTfaDefaultOtpChannelType();
            channelTypeOptional = OtpChannelType.valueOfTypeName(tenantOtpChannelTypeStr);
        }

        return channelTypeOptional.orElseThrow(
            () -> new InternalAuthenticationServiceException("User (key: " + userDetails.getUserKey()
                                                                 + ") and tenant: " + userDetails.getTenant()
                                                                 + " config both has no configured OTP sender channel")
        );
    }

    @Getter
    @RequiredArgsConstructor
    private static class OtpChannel {

        private final OtpChannelType type;
        private final String destination;

    }

}
