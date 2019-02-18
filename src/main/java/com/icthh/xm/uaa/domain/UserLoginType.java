package com.icthh.xm.uaa.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public enum UserLoginType {
    EMAIL("LOGIN.EMAIL"),
    MSISDN("LOGIN.MSISDN"),
    NICKNAME("LOGIN.NICKNAME");

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

        for (UserLoginType type : UserLoginType.values()) {
            if (Objects.equals(value, type.getValue())) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

}
