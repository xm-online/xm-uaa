package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * A DTO representing a user, with his authorities.
 */
@AllArgsConstructor
@ToString(exclude = {"firstName", "lastName"})
@Getter
@Setter
public class UserPublicDTO {

    private String userKey;

    private String firstName;

    private String lastName;

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
