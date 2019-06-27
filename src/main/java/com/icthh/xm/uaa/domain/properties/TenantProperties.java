package com.icthh.xm.uaa.domain.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.security.ldap.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"security"})
@Getter
@Setter
@ToString
public class TenantProperties {

    @JsonProperty("security")
    private Security security;

    @Getter
    @Setter
    @ToString
    public static class Security {

        @JsonProperty("defaultUserRole")
        private String defaultUserRole;

        @JsonProperty("tfaEnabled")
        private boolean tfaEnabled = false;

        @JsonProperty("tfaDefaultOtpChannelType")
        private String tfaDefaultOtpChannelType;

        @JsonProperty("tfaEnabledOtpChannelTypes")
        private Set<OtpChannelType> tfaEnabledOtpChannelTypes = new LinkedHashSet<>();

        @JsonProperty("tfaAccessTokenValiditySeconds")
        private Integer tfaAccessTokenValiditySeconds;

        @JsonProperty("accessTokenValiditySeconds")
        private Integer accessTokenValiditySeconds;

        @JsonProperty("refreshTokenValiditySeconds")
        private Integer refreshTokenValiditySeconds;

        @JsonProperty("defaultClientSecret")
        private String defaultClientSecret;

        @JsonSetter("accessTokenValiditySeconds")
        public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
            this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        }

        @JsonSetter("refreshTokenValiditySeconds")
        public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }
    }

    private PublicSettings publicSettings;

    @Data
    public static class PublicSettings {

        private PasswordSettings passwordSettings;

        @Data
        public static class PasswordSettings {
            private byte minLength = 0;
            private byte maxLength = Byte.MAX_VALUE;
            private String pattern;
            private String patternMessage;
            private boolean enableBackEndValidation = false;
        }
    }

    @JsonProperty("social")
    private List<Social> social;

    @JsonProperty("socialBaseUrl")
    private String socialBaseUrl;

    @Data
    @ToString(of = {"providerId"})
    public static class Social {

        private String providerId;
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String accessTokenUrl;
        private String scope;
        private String userInfoUri;
        private UserInfoMapping userInfoMapping;
        private String tokenStrategy;
        private Boolean createAccountAutomatically;
        private Boolean useParametersForClientAuthentication;

        public Boolean getUseParametersForClientAuthentication() {
            return ofNullable(useParametersForClientAuthentication).orElse(true);
        }

        public Boolean getCreateAccountAutomatically() {
            return ofNullable(createAccountAutomatically).orElse(true);
        }
    }

    @Data
    @NoArgsConstructor
    public static class UserInfoMapping {
        private String id;
        private String name;
        private String firstName;
        private String lastName;
        private String email;
        private String username;

        private String profileUrl;
        private String imageUrl;

        private String phoneNumber;
        private String langKey;

        // It's field detect is email verified in provider.
        // If field null or empty verification wiil be disabled.
        private String emailVerifiedCheckField;
    }

    @JsonProperty("registrationCaptchaPeriodSeconds")
    private Long registrationCaptchaPeriodSeconds;

    @JsonProperty("activationKeyLifeTime")
    private Long activationKeyLifeTime;

    @JsonProperty("resetKeyLifeTime")
    private Long resetKeyLifeTime;

    @JsonProperty("ldap")
    private List<Ldap> ldap = new ArrayList<>();

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString(exclude = "systemPassword")
    public static class Ldap {

        private String domain;
        private Type type;
        private String providerUrl;
        private String systemUser;
        private String systemPassword;
        private String rootDn;
        private String groupSearchBase;
        private Boolean groupSearchSubtree;
        private String userDnPattern;
        private Role role = new Role();
        private Attribute attribute = new Attribute();

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class Attribute {

            private String firstName;
            private String lastName;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @ToString
        public static class Role {

            private String defaultRole;
            private Map<String, String> mapping;
        }
    }
}
