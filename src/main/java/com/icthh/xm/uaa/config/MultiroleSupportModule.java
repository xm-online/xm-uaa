package com.icthh.xm.uaa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiroleSupportModule extends SimpleModule {

    public MultiroleSupportModule() {
        addDeserializer(UserDTO.class, new UserDtoDeserializer());
    }

    @RequiredArgsConstructor
    public static class UserDtoDeserializer extends JsonDeserializer<UserDTO> {

        @Override
        @SneakyThrows
        public UserDTO deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            UserDTOJsonBean deserialize = jsonParser.readValueAs(UserDTOJsonBean.class);
            deserialize.setRoleKey(deserialize.originRoleKey);
            return deserialize;
        }

        public static class UserDTOJsonBean extends UserDTO {
            @JsonIgnore
            private transient String originRoleKey;

            @Override
            public void setRoleKey(String roleKey) {
                super.setRoleKey(roleKey);
                this.originRoleKey = roleKey;
            }
        }
    }
}
