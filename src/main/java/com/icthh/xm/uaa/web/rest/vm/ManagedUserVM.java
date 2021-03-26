package com.icthh.xm.uaa.web.rest.vm;

import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * View Model extending the UserDTO, which is meant to be used in the user management UI.
 */
public class ManagedUserVM extends UserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    @NotBlank
    @Getter
    @Setter
    private String password;

    @Setter
    @Getter
    private String captcha;

    @SuppressWarnings("unused")
    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    /**
     * Constructor.
     *
     * @param id
     * @param password
     * @param firstName
     * @param lastName
     * @param activated
     * @param tfaEnabled
     * @param tfaOtpChannelType
     * @param tfaOtpChannelSpec
     * @param imageUrl
     * @param langKey
     * @param createdBy
     * @param createdDate
     * @param lastModifiedBy
     * @param lastModifiedDate
     * @param userKey
     * @param roleKey
     * @param accessTokenValiditySeconds
     * @param refreshTokenValiditySeconds
     * @param tfaAccessTokenValiditySeconds
     * @param data
     * @param logins
     * @param autoLogoutEnabled
     * @param autoLogoutTimeoutSeconds
     */
    public ManagedUserVM(Long id,
                         String password,
                         String firstName,
                         String lastName,
                         boolean activated,
                         boolean tfaEnabled,
                         OtpChannelType tfaOtpChannelType,
                         TfaOtpChannelSpec tfaOtpChannelSpec,
                         String imageUrl,
                         String langKey,
                         String createdBy,
                         Instant createdDate,
                         String lastModifiedBy,
                         Instant lastModifiedDate,
                         String userKey,
                         String roleKey,
                         Integer accessTokenValiditySeconds,
                         Integer refreshTokenValiditySeconds,
                         Integer tfaAccessTokenValiditySeconds,
                         Map<String, Object> data,
                         List<UserLogin> logins,
                         boolean autoLogoutEnabled,
                         Integer autoLogoutTimeoutSeconds,
                         Instant acceptTocTime,
                         List<String> authorities) {

        super(id,
            firstName,
            lastName,
            imageUrl,
            activated,
            tfaEnabled,
            tfaOtpChannelType,
            tfaOtpChannelSpec,
            langKey,
            createdBy,
            createdDate,
            lastModifiedBy,
            lastModifiedDate,
            userKey,
            roleKey,
            authorities,
            accessTokenValiditySeconds,
            refreshTokenValiditySeconds,
            tfaAccessTokenValiditySeconds,
            data,
            logins,
            null,
            autoLogoutEnabled,
            autoLogoutTimeoutSeconds,
            acceptTocTime);
        this.password = password;
    }

    @Override
    public String toString() {
        return "ManagedUserVM{"
            + "} " + super.toString();
    }

}
