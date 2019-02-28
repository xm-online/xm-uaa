package com.icthh.xm.uaa.domain.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"logins"})
@Data
public class TenantLogins {

    @JsonProperty("logins")
    private List<Login> logins;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"key", "name"})
    @Data
    public static class Login {

        @JsonProperty("key")
        private String key;
        @JsonProperty("name")
        private Map<String, String> name;
    }
}
