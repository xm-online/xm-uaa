package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link OtpChannelType} enum.
 */
public enum OtpChannelType {

    SMS("sms"),
    EMAIL("email"),
    TELEGRAM("telegram"),
    SKYPE("skype"),
    VIBER("viber"),
    WHATSAPP("whatsapp"),
    SLACK("slack"),
    HANGOUTS("hangouts");

    @JsonValue
    private final String typeName;

    OtpChannelType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public static Optional<OtpChannelType> valueOfTypeName(String typeName) {
        if (StringUtils.isBlank(typeName)) {
            return Optional.empty();
        }

        for (OtpChannelType providerType : OtpChannelType.values()) {
            if (Objects.equals(providerType.getTypeName().toLowerCase(), typeName.toLowerCase())) {
                return Optional.of(providerType);
            }
        }

        return Optional.empty();
    }

    @JsonCreator
    public static OtpChannelType fromJson(String jsonValue) {
        return valueOfTypeName(jsonValue).orElseThrow(
            () -> new IllegalArgumentException("Unsupported JSON value for OtpChannelType: '" + jsonValue + "'")
        );
    }

    @JsonValue
    public String toJson() {
        return getTypeName();
    }

}
