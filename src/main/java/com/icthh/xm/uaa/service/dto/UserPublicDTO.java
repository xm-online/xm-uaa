package com.icthh.xm.uaa.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.util.OtpUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DTO representing a user, with his authorities.
 */
@AllArgsConstructor
@ToString(exclude = {"permissions"})
@Getter
@Setter
public class UserPublicDTO {

    private String userKey;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 256)
    private String imageUrl;

    private boolean activated = false;

    /**
     * UserDTO constructor.
     *
     * @param user user
     */
    public UserPublicDTO(User user) {
        this(
             user.getUserKey(),
             user.getFirstName(),
             user.getLastName(),
             user.getImageUrl(),
             user.isActivated()
        );
    }

}
