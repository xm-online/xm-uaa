package com.icthh.xm.uaa.service.dto;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.util.OtpUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A DTO representing a user, with his authorities.
 */
@AllArgsConstructor
@ToString(exclude = {"permissions"})
@Getter
@Setter
public class UserDTO {

    private Long id;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 256)
    private String imageUrl;

    private boolean activated = false;

    /**
     * Flag is TFA enabled for user. Use as read only!.
     */
    private boolean tfaEnabled = false;

    /**
     * User OTP channel type. Can be null. Use as read only!
     */
    private OtpChannelType tfaOtpChannelType;

    /**
     * Current user TFA channel. Can be null. Use as read only!
     */
    private TfaOtpChannelSpec tfaOtpChannelSpec;

    @Size(min = 2, max = 5)
    private String langKey;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    private String userKey;

    private String roleKey;
    private List<String> authorities;
    private Integer accessTokenValiditySeconds;

    private Integer refreshTokenValiditySeconds;

    private Integer tfaAccessTokenValiditySeconds;

    private Map<String, Object> data = new HashMap<>();

    @NotEmpty
    @Valid
    private List<UserLogin> logins;

    private List<AccPermissionDTO> permissions;

    private boolean autoLogoutEnabled = false;

    private Integer autoLogoutTimeoutSeconds;

    private Instant acceptTocTime;

    @SuppressWarnings("unused")
    public UserDTO() {
        // Empty constructor needed for Jackson.
    }

    /**
     * UserDTO constructor.
     *
     * @param user user
     */
    public UserDTO(User user) {
        this(user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getImageUrl(),
            user.isActivated(),
            user.isTfaEnabled(),
            user.getTfaOtpChannelType(),
            null,
            user.getLangKey(),
            user.getCreatedBy(),
            user.getCreatedDate(),
            user.getLastModifiedBy(),
            user.getLastModifiedDate(),
            user.getUserKey(),
            getFirst(user.getAuthorities(), null),
            user.getAuthorities(),
            user.getAccessTokenValiditySeconds(),
            user.getRefreshTokenValiditySeconds(),
            user.getTfaAccessTokenValiditySeconds(),
            user.getData(),
            user.getLogins(),
            new ArrayList<>(),
            user.isAutoLogoutEnabled(),
            user.getAutoLogoutTimeoutSeconds(),
            user.getAcceptTocTime()
        );
        OtpUtils.enrichTfaOtpChannelSpec(this);
    }

    // TODO refactor, put EMAIL type to configuration
    @JsonIgnore
    public String getEmail() {
        return getLogins().stream().filter(userLogin -> UserLoginType.EMAIL.getValue().equals(userLogin.getTypeKey()))
            .findFirst().map(UserLogin::getLogin).orElse(null);
    }

    public void setAuthorities(List<String> authorities){
        this.authorities = authorities;
        this.roleKey = getFirst(authorities, null);
    }

    public List<String> getAuthorities(){
        return isEmpty(this.authorities) && roleKey != null ? List.of(this.roleKey) : authorities;
    }

}
