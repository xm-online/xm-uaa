package com.icthh.xm.uaa.repository.converter;

import com.icthh.xm.uaa.domain.OtpChannelType;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * The {@link OtpChannelTypeAttributeConverterUnitTest} class.
 */
public class OtpChannelTypeAttributeConverterUnitTest {

    @Test
    public void sureIsAllValuesConvertedWithoutException() {
        OtpChannelTypeAttributeConverter converter = new OtpChannelTypeAttributeConverter();

        assertNull(converter.convertToDatabaseColumn(null));

        for (OtpChannelType otpChannelType : OtpChannelType.values()) {
            converter.convertToDatabaseColumn(otpChannelType);
        }
    }

}
