package com.icthh.xm.uaa.domain;

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
}
