package com.icthh.xm.uaa.service.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class UserPassDto implements Serializable {

    private String encryptedPassword;
    private Boolean setByUser;
}
