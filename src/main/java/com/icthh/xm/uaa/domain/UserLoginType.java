package com.icthh.xm.uaa.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public enum UserLoginType {
    EMAIL("LOGIN.EMAIL"),
    MSISDN("LOGIN.MSISDN"),
    NICKNAME("LOGIN.NICKNAME");

    private static final Set<UserLoginType> VALUES = Set.of(values());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,4}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,15}$");

    private final String value;

    UserLoginType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<UserLoginType> valueOfType(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        for (UserLoginType type : VALUES) {
            if (Objects.equals(value, type.getValue())) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    public static UserLoginType fromString(String value) {
        return VALUES.stream()
                     .filter(UserLoginType -> UserLoginType.getValue().equalsIgnoreCase(value))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException(
                         "can not build UserLoginType from value: " + value));
    }

    public static UserLoginType getByRegex(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("can not build UserLoginType from value: " + value);
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return UserLoginType.EMAIL;
        }
        if (PHONE_PATTERN.matcher(value).matches()) {
            return UserLoginType.MSISDN;
        }
        throw new IllegalArgumentException("can not build UserLoginType from value: " + value);
    }

}
