package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GrantType {

    OTP("otp"),
    PASSWORD("password");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    public static GrantType fromGrantType(String value) {
        return Arrays.stream(GrantType.values())
            .filter(v -> v.getValue().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid grant type: " + value));
    }
}
