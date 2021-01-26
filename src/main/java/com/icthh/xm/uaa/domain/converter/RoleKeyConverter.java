package com.icthh.xm.uaa.domain.converter;


import static com.google.common.collect.Iterables.getLast;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.io.IOException;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
@Slf4j
public class RoleKeyConverter implements AttributeConverter<List<String>, String> {

    private TenantPropertiesService tenantPropertiesService;
    private ObjectMapper mapper = new ObjectMapper();

    {
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<String> s) {
        return toJson(s);
    }

    @Override
    public List<String> convertToEntityAttribute(String roles) {
        List<String> multiRoles = fromJson(roles);
        if(multiRoles.isEmpty()) return multiRoles;
        return tenantPropertiesService.getTenantProps().isMultiRoleEnabled() ?
            multiRoles :
            List.of(getLast(multiRoles));
    }

    @Autowired
    public void setTenantPropertiesService(TenantPropertiesService tenantPropertiesService) {
        this.tenantPropertiesService = tenantPropertiesService;
    }

    public String toJson(List<String> data) {
        try {
            return mapper.writeValueAsString(data != null ? data : emptyList());
        } catch (JsonProcessingException e) {
            log.warn("Error during JSON to String converting", e);
        }
        return "";
    }

    public List<String> fromJson(String dbValue) {
        try {
            return mapper.readValue(isNoneBlank(dbValue) ? dbValue : "[]", new TypeReference<>() {
            });
        } catch (IOException e) {
            log.warn("Error during String to JSON converting", e);
        }
        return emptyList();
    }
}



