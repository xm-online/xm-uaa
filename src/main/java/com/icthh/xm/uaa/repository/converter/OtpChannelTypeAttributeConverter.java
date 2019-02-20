package com.icthh.xm.uaa.repository.converter;

import com.icthh.xm.uaa.domain.OtpChannelType;

import javax.persistence.AttributeConverter;

/**
 * The {@link OtpChannelTypeAttributeConverter} class.
 */
public class OtpChannelTypeAttributeConverter implements AttributeConverter<OtpChannelType, String> {

    @Override
    public String convertToDatabaseColumn(OtpChannelType attribute) {
        return (attribute != null) ? attribute.getTypeName() : null;
    }

    @Override
    public OtpChannelType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return OtpChannelType.valueOfTypeName(dbData).orElseThrow(
            () -> new IllegalStateException("Unsupported OTP channel type name in DB '" + dbData + "'")
        );
    }

}
