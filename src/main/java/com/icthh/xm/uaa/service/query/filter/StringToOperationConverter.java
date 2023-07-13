package com.icthh.xm.uaa.service.query.filter;

import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToOperationConverter implements Converter<String, Operation> {
    @Override
    public Operation convert(String source) {
        return Operation.fromOperation(source);
    }
}
