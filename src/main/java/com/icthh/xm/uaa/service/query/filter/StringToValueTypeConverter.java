package com.icthh.xm.uaa.service.query.filter;

import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToValueTypeConverter implements Converter<String, ValueType> {
    @Override
    public ValueType convert(String source) {
        return ValueType.fromValueType(source);
    }
}
