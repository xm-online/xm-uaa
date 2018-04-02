package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.commons.permission.domain.Permission;
import lombok.Getter;
import lombok.ToString;

/**
 *
 */
@ToString
@Getter
public class AccPermissionDTO {

    private String msName;
    private String roleKey;
    private String privilegeKey;
    private boolean enabled;

    @SuppressWarnings("unused")
    public AccPermissionDTO(){
        // Empty constructor needed for Jackson.
    }

    public AccPermissionDTO(Permission permission) {
        this.msName = permission.getMsName();
        this.roleKey = permission.getRoleKey();
        this.privilegeKey = permission.getPrivilegeKey();
        this.enabled = !permission.isDisabled();
    }
}
