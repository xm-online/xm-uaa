package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OauthCredentials {

    private Uaa uaa;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Uaa {
        private String defaultClientId;
        private String defaultClientSecret;

    }
}
