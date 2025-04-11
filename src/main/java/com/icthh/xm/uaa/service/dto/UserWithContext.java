package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * This class extends the UserDTO with permission context
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class UserWithContext extends UserDTO {

    private Map<String, Object> context = new HashMap<>();

    public UserWithContext(User user) {
        super(user);
    }
}
