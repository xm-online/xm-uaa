package com.icthh.xm.uaa.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
@Converter
public class ListToStringConverter implements AttributeConverter<List<String>, String> {

    private ObjectMapper mapper = new ObjectMapper();

    public ListToStringConverter() {
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<String> data) {
        String value = "";
        try {
            value = mapper.writeValueAsString(data != null ? data : emptyList());
        } catch (JsonProcessingException e) {
            log.warn("Error during JSON to String converting", e);
        }
        return value;
    }

    @Override
    public List<String> convertToEntityAttribute(String dbValue) {
        List<String> mapValue = emptyList();
        TypeReference<List<String>> typeRef = new TypeReference<>() {
        };
        try {
            mapValue = mapper.readValue(StringUtils.isNoneBlank(dbValue) ? dbValue : "[]", typeRef);
        } catch (IOException e) {
            log.warn("Error during String to JSON converting", e);
        }
        return mapValue;
    }

}
