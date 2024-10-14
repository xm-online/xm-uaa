package com.icthh.xm.uaa.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.icthh.xm.uaa.config.Constants.LOGIN_INVALID_CODE;
import static com.icthh.xm.uaa.config.Constants.OTP_THROTTLING_ERROR_TEXT;

/**
 * The {@link OtpUtils} class.
 */
public final class OtpUtils {

    private static final Map<UserLoginType, List<OtpChannelType>> LOGIN_TYPE_TO_CHANNELS;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,4}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,15}$");

    static {
        Map<UserLoginType, List<OtpChannelType>> map = new HashMap<>();
        map.put(UserLoginType.EMAIL, Collections.singletonList(OtpChannelType.EMAIL));
        map.put(UserLoginType.MSISDN, Arrays.asList(OtpChannelType.SMS, OtpChannelType.VIBER, OtpChannelType.WHATSAPP));

        LOGIN_TYPE_TO_CHANNELS = Collections.unmodifiableMap(map);
    }

    private static final Map<OtpChannelType, Collection<UserLoginType>> CHANNEL_TO_LOGIN_TYPES;

    static {
        Map<OtpChannelType, Collection<UserLoginType>> map = new HashMap<>();
        for (Map.Entry<UserLoginType, List<OtpChannelType>> entry : LOGIN_TYPE_TO_CHANNELS.entrySet()) {
            for (OtpChannelType channelType : entry.getValue()) {
                Collection<UserLoginType> dstLoginTypes = map.computeIfAbsent(channelType, otpChannelType -> new LinkedHashSet<>());
                dstLoginTypes.add(entry.getKey());
            }
        }
        CHANNEL_TO_LOGIN_TYPES = Collections.unmodifiableMap(map);
    }

    public static Optional<List<OtpChannelType>> getSupportedOtpChannelTypes(UserLoginType loginType) {
        return Optional.ofNullable(LOGIN_TYPE_TO_CHANNELS.get(loginType));
    }

    public static UserLoginType getRequiredLoginType(String loginTypeKeyStr) {
        return UserLoginType.valueOfType(loginTypeKeyStr).orElseThrow(
            () -> new IllegalArgumentException("Unsupported user login type: " + loginTypeKeyStr)
        );
    }

    public static Optional<Collection<UserLoginType>> getSupportedUserLoginTypes(OtpChannelType otpChannelType) {
        return Optional.ofNullable(CHANNEL_TO_LOGIN_TYPES.get(otpChannelType));
    }

    public static UserDTO enrichTfaOtpChannelSpec(UserDTO userDto) {
        if (userDto == null) {
            return null;
        }

        OtpChannelType tfaOtpChannelType = userDto.getTfaOtpChannelType();
        if (tfaOtpChannelType == null || CollectionUtils.isEmpty(userDto.getLogins())) {
            return userDto;
        }

        Optional<Collection<UserLoginType>> supportedUserLoginTypes = OtpUtils.getSupportedUserLoginTypes(tfaOtpChannelType);
        if (!supportedUserLoginTypes.isPresent()) {
            return userDto;
        }

        UserLogin userLoginForChannel = null;
        for (UserLoginType userLoginType : supportedUserLoginTypes.get()) {
            for (UserLogin userLogin : userDto.getLogins()) {
                if (userLoginType.getValue().equals(userLogin.getTypeKey())) {
                    userLoginForChannel = userLogin;
                    break;
                }
            }

            if (userLoginForChannel != null) {
                break;
            }
        }

        if (userLoginForChannel != null) {
            TfaOtpChannelSpec spec = new TfaOtpChannelSpec(tfaOtpChannelType, userLoginForChannel.getLogin());
            userDto.setTfaOtpChannelSpec(spec);
        }

        return userDto;
    }

    public static OtpChannelType getOptChanelTypeByLogin(String login) {
        if (StringUtils.isEmpty(login)) {
            throw new BusinessException(LOGIN_INVALID_CODE, Constants.LOGIN_INVALID_ERROR_TEXT);
        }
        if (EMAIL_PATTERN.matcher(login).matches()) {
            return OtpChannelType.EMAIL;
        }
        if (PHONE_PATTERN.matcher(login).matches()) {
            return OtpChannelType.SMS;
        }
        throw new BusinessException(LOGIN_INVALID_CODE, Constants.LOGIN_INVALID_ERROR_TEXT);
    }

    public static void validateOptCodeSentInterval(TenantProperties tenantProps, Instant otpCodeCreationDate) {
        if (otpCodeCreationDate == null) {
            return;
        }
        Duration actualInterval = Duration.between(otpCodeCreationDate, Instant.now());
        Integer configuredInterval = tenantProps.getSecurity().getOtpThrottlingTimeIntervalInSeconds();
        int allowedInterval = configuredInterval != null ? configuredInterval : 30;

        if (allowedInterval >= 0 && actualInterval.getSeconds() < allowedInterval) {
            throw new BusinessException(OTP_THROTTLING_ERROR_TEXT);
        }
    }
}
