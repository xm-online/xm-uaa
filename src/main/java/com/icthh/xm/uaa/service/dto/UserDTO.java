package com.icthh.xm.uaa.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.uaa.domain.Authority;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A DTO representing a user, with his authorities.
 */
@AllArgsConstructor
@ToString
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

    @Size(min = 2, max = 5)
    private String langKey;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    private Set<String> authorities;

    private String userKey;

    private Integer accessTokenValiditySeconds;

    private Integer refreshTokenValiditySeconds;

    private Map<String, Object> data = new HashMap<>();

    @NotEmpty
    @Valid
    private List<UserLogin> logins;

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
        this(user.getId(), user.getFirstName(), user.getLastName(),
            user.getImageUrl(), user.isActivated(), user.getLangKey(),
            user.getCreatedBy(), user.getCreatedDate(), user.getLastModifiedBy(), user.getLastModifiedDate(),
            user.getAuthorities().stream().map(Authority::getName)
                .collect(Collectors.toSet()), user.getUserKey(), user.getAccessTokenValiditySeconds(),
            user.getRefreshTokenValiditySeconds(), user.getData(), user.getLogins());
    }

    //TODO refactor, put EMAIL type to configuration
    @JsonIgnore
    public String getEmail() {
        return getLogins().stream().filter(userLogin -> UserLoginType.EMAIL.getValue().equals(userLogin.getTypeKey()))
            .findFirst().map(UserLogin::getLogin).orElse(null);
    }

}
