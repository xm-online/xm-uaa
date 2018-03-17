package com.icthh.xm.uaa.service.mapper;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.dto.UserDTO;
import org.springframework.stereotype.Service;

/**
 * Mapper for the entity User and its DTO called UserDTO.
 * Normal mappers are generated using MapStruct, this one is hand-coded as MapStruct
 * support is still in beta, and requires a manual step with an IDE.
 */
@Service
public class UserMapper {

    public UserDTO userToUserDTO(User user) {
        return new UserDTO(user);
    }

    /**
     * Map userDTO to User.
     * @param userDTO userDTO
     * @return User
     */
    public User userDTOToUser(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        } else {
            User user = new User();
            user.setId(userDTO.getId());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setImageUrl(userDTO.getImageUrl());
            user.setActivated(userDTO.isActivated());
            user.setLangKey(userDTO.getLangKey());
            user.setRoleKey(userDTO.getRoleKey());
            return user;
        }
    }
}
