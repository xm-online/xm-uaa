package com.icthh.xm.uaa.domain.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"security"})
@Data
public class TenantProperties {

    @JsonProperty("security")
    private Security security;

    @Data
    public static class Security {

        @JsonProperty("accessTokenValiditySeconds")
        private Integer accessTokenValiditySeconds;

        @JsonProperty("refreshTokenValiditySeconds")
        private Integer refreshTokenValiditySeconds;

        @JsonSetter("accessTokenValiditySeconds")
        public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
            this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        }

        // TODO FIXME remove after fix all config in prod
        @JsonSetter("access-token-validity-seconds")
        public void setAccessTokenValiditySecondsAlias(Integer accessTokenValiditySeconds) {
            this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        }

        @JsonSetter("refreshTokenValiditySeconds")
        public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }

        // TODO FIXME remove after fix all config in prod
        @JsonSetter("refresh-token-validity-seconds")
        public void setRefreshTokenValiditySecondsAlias(Integer refreshTokenValiditySeconds) {
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }
    }

    @JsonProperty("social")
    private List<Social> social;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Social {

        private String providerId;
        private String consumerKey;
        private String consumerSecret;
        private String domain;

    }

    @JsonProperty("registrationCaptchaPeriodSeconds")
    private Long registrationCaptchaPeriodSeconds;

}
