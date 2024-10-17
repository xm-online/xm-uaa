package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.service.dto.UserLoginDto;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Getter
public class DomainUserDetails extends User {

    private final boolean tfaEnabled;
    private final String tfaOtpSecret;
    private final Integer tfaAccessTokenValiditySeconds;

    private String authOtpCode;
    private String tfaEncodedOtp;
    private OtpChannelType tfaOtpChannelType;

    private Long otpId;
    private String langKey;

    private final String tenant;
    private final String userKey;
    private final Integer accessTokenValiditySeconds;
    private final Integer refreshTokenValiditySeconds;
    private final List<UserLoginDto> logins;
    private final Map<String, String> additionalDetails = new HashMap<>();

    private final boolean autoLogoutEnabled;
    private final Integer autoLogoutTimeoutSeconds;

    public DomainUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String tenant,
                             String userKey,
                             boolean tfaEnabled,
                             String tfaOtpSecret,
                             OtpChannelType tfaOtpChannelType,
                             boolean autoLogoutEnabled,
                             Integer autoLogoutTimeoutSeconds) {
        this(username,
             password,
             authorities,
             tenant,
             userKey,
             tfaEnabled,
             tfaOtpSecret,
             tfaOtpChannelType,
             null,
             null,
             null,
             autoLogoutEnabled,
             autoLogoutTimeoutSeconds,
             emptyList());
    }

    public DomainUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String tenant,
                             String userKey,
                             boolean tfaEnabled,
                             String tfaOtpSecret,
                             OtpChannelType tfaOtpChannelType,
                             Integer accessTokenValiditySeconds,
                             Integer refreshTokenValiditySeconds,
                             Integer tfaAccessTokenValiditySeconds,
                             boolean autoLogoutEnabled,
                             Integer autoLogoutTimeoutSeconds,
                             List<UserLoginDto> logins) {
        super(username,
              password,
              true,
              true,
              true,
              true,
              authorities);
        this.tenant = tenant;
        this.userKey = userKey;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        this.logins = logins;
        this.tfaEnabled = tfaEnabled;
        this.tfaOtpSecret = tfaOtpSecret;
        this.tfaOtpChannelType = tfaOtpChannelType;
        this.tfaAccessTokenValiditySeconds = tfaAccessTokenValiditySeconds;

        this.autoLogoutEnabled = autoLogoutEnabled;
        this.autoLogoutTimeoutSeconds = autoLogoutTimeoutSeconds;
    }

    public DomainUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String tenant,
                             String userKey,
                             String authOtpCode,
                             boolean tfaEnabled,
                             String tfaOtpSecret,
                             OtpChannelType tfaOtpChannelType,
                             Integer accessTokenValiditySeconds,
                             Integer refreshTokenValiditySeconds,
                             Integer tfaAccessTokenValiditySeconds,
                             boolean autoLogoutEnabled,
                             Integer autoLogoutTimeoutSeconds,
                             List<UserLoginDto> logins,
                             String langKey) {
        super(username,
              password,
              true,
              true,
              true,
              true,
              authorities);
        this.tenant = tenant;
        this.userKey = userKey;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        this.logins = logins;
        this.authOtpCode = authOtpCode;
        this.tfaEnabled = tfaEnabled;
        this.tfaOtpSecret = tfaOtpSecret;
        this.tfaOtpChannelType = tfaOtpChannelType;
        this.tfaAccessTokenValiditySeconds = tfaAccessTokenValiditySeconds;

        this.autoLogoutEnabled = autoLogoutEnabled;
        this.autoLogoutTimeoutSeconds = autoLogoutTimeoutSeconds;
        this.langKey = langKey;
    }

    public Optional<String> getTfaEncodedOtp() {
        return Optional.ofNullable(tfaEncodedOtp);
    }

    public void setTfaEncodedOtp(String tfaEncodedOtp) {
        this.tfaEncodedOtp = tfaEncodedOtp;
    }

    public Long getOtpId() {
        return otpId;
    }

    public void setOtpId(Long otpId) {
        this.otpId = otpId;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public Optional<OtpChannelType> getTfaOtpChannelType() {
        return Optional.ofNullable(tfaOtpChannelType);
    }

    public void setTfaOtpChannelType(OtpChannelType tfaOtpChannelType) {
        this.tfaOtpChannelType = tfaOtpChannelType;
    }

    public boolean isTfaApplied() {
        return getTfaEncodedOtp().isPresent();
    }

    public boolean isOtpIdPresent() {
        return otpId != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DomainUserDetails rhs = (DomainUserDetails) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(tenant, rhs.tenant)
            .append(userKey, rhs.userKey)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(this.tenant.hashCode())
            .append(this.userKey.hashCode())
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("tenant", this.tenant)
            .append("userKey", this.userKey)
            .append("tfaEnabled", this.tfaEnabled)
            .build();
    }

}
