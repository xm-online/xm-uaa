package com.icthh.xm.uaa.service.otp;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReceiverTypeKey {

    USER_ID("USER-ID"),
    EMAIL("EMAIL"),
    IP("IP"),
    NAME("NAME"),
    PHONE_NUMBER("PHONE-NUMBER");

    private final String value;

    ReceiverTypeKey(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
