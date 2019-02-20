package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.commons.permission.domain.Role;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class RoleDTO {

    @NotEmpty
    private String roleKey;
    private String basedOn;
    private String description;
    private String createdDate;
    private String createdBy;
    private String updatedDate;
    private String updatedBy;

    private Collection<PermissionDTO> permissions;

    private List<String> env;

    public RoleDTO() {
    }

    public RoleDTO(Role role) {
        roleKey = role.getKey();
        description = role.getDescription();
        createdDate = role.getCreatedDate();
        createdBy = role.getCreatedBy();
        updatedDate = role.getUpdatedDate();
        updatedBy = role.getUpdatedBy();
    }

    @Override
    public String toString() {
        return "RoleDTO{" +
               "roleKey='" + roleKey + '\'' +
               ", basedOn='" + basedOn + '\'' +
               ", description='" + description + '\'' +
               ", createdDate='" + createdDate + '\'' +
               ", createdBy='" + createdBy + '\'' +
               ", updatedDate='" + updatedDate + '\'' +
               ", updatedBy='" + updatedBy + '\'' +
               ", permissions.size=" + (permissions != null ? permissions.size() : null) +
               ", env=" + env +
               '}';
    }
}
