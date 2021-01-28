package com.icthh.xm.uaa.domain.properties;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.UserSpec;
import com.icthh.xm.uaa.security.ldap.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"security"})
@Getter
@Setter
@ToString
public class TenantProperties {

    /**
     * This flag disables RoleUpdate and StateUpdate by UserUpdateAction.
     * If strictUserManagement no changes to Role or State will be applied
     * If !strictUserManagement log.warn will be written but changes will be applied
     */
    @JsonProperty("strictUserManagement")
    private boolean strictUserManagement;

    @JsonProperty("security")
    private Security security = new Security();

    @JsonProperty("userSpec")
    private List<UserSpec> userSpec;


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

        @JsonProperty("passwordExpirationPeriod")
        private Integer passwordExpirationPeriod = -1;

        @JsonProperty("removeDefaultPermissions")
        private Boolean removeDefaultPermissions = false;

        @JsonSetter("accessTokenValiditySeconds")
        public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
            this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        }

        @JsonSetter("refreshTokenValiditySeconds")
        public void setRefreshTokenValiditySeconds(Integer refreshTokenValiditySeconds) {
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }

        @JsonProperty("idp")
        private Idp idp;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Idp {

            @JsonProperty("defaultIdpClaimMapping")
            private DefaultIdpClaimMapping defaultIdpClaimMapping;

            @Data
            public static class DefaultIdpClaimMapping {
                private String userIdentityAttribute;
                private String userIdentityType;
                private String firstNameAttribute;
                private String lastNameAttribute;
            }
        }

    }

    private PublicSettings publicSettings;

    @Data
    public static class PublicSettings {

        private PasswordSettings passwordSettings;
        private List<PasswordPolicy> passwordPolicies;
        private Long passwordPoliciesMinimalMatchCount;

        private Boolean termsOfConditionsEnabled;

        @Data
        public static class PasswordSettings {
            private int minLength = 0;
            private int maxLength = Byte.MAX_VALUE;
            private String pattern;
            private Map<String, String> patternMessage;
            private boolean enableBackEndValidation = false;
        }

        @Data
        public static class PasswordPolicy {
            private String pattern;
            private Map<String, String> patternMessage;
        }

        public Boolean isTermsOfConditionsEnabled() {
            return TRUE.equals(termsOfConditionsEnabled);
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

    @JsonProperty("ldapSearchTemplates")
    private List<LdapSearchTemplate> ldapSearchTemplates = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString(exclude = "systemPassword")
    public static class Ldap {

        private String domain;
        private Type type;
        private String imageUrl;
        private String dynamicParameterPattern;
        private String providerUrl;
        private String systemUser;
        private String systemPassword;
        private String rootDn;
        private String groupSearchBase;
        private Boolean groupSearchSubtree;
        private String userDnPattern;
        private Role role = new Role();
        private String searchFields;
        private Boolean useNameWithoutDomain;
        private String authField;
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

    @Data
    public static class LdapSearchTemplate {
        private String templateKey;
        private String query;
        private String domain;
        private String[] attributeNames;
    }

}
