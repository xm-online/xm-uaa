package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.UserLogin;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.Column;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class UserLoginDto {

    private String typeKey;
    private String stateKey;
    private String login;

    public UserLoginDto(UserLogin userLogin) {
        this.typeKey = userLogin.getTypeKey();
        this.stateKey = userLogin.getStateKey();
        this.login = userLogin.getLogin();
    }
}
