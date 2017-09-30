package com.icthh.xm.uaa.web.rest.vm;

import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.Getter;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * View Model extending the UserDTO, which is meant to be used in the user management UI.
 */
public class ManagedUserVM extends UserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    @NotBlank
    private String password;

    @Getter
    private String captcha;

    @SuppressWarnings("unused")
    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    /**
     * Constructor.
     *
     * @param id id
     * @param password password
     * @param firstName first name
     * @param lastName last name
     * @param activated activated
     * @param imageUrl image url
     * @param langKey langKey
     * @param createdBy createdBy
     * @param createdDate createdDate
     * @param lastModifiedBy lastModifiedBy
     * @param lastModifiedDate lastModifiedDate
     * @param authorities authorities
     * @param userKey user key
     * @param logins logins
     */
    public ManagedUserVM(Long id, String password, String firstName, String lastName,
                         boolean activated, String imageUrl, String langKey,
                         String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate,
                         Set<String> authorities, String userKey, Integer accessTokenValiditySeconds,
                         Integer refreshTokenValiditySeconds, Map<String, Object> data, List<UserLogin> logins) {

        super(id, firstName, lastName, imageUrl, activated, langKey,
            createdBy, createdDate, lastModifiedBy, lastModifiedDate, authorities, userKey, accessTokenValiditySeconds,
            refreshTokenValiditySeconds, data, logins);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ManagedUserVM{"
            + "} " + super.toString();
    }
}
