package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.commons.permission.domain.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@Data
@NoArgsConstructor
public class RoleDTO {

    @NotEmpty
    private String roleKey;
    private String basedOn;
    private String description;
    private String createdDate;
    private String createdBy;
    private String updatedDate;
    private String updatedBy;

    private Collection<PermissionDTO> permissions = new TreeSet<>();
    private List<String> env = new ArrayList<>();

    public RoleDTO(Role role) {
        roleKey = role.getKey();
        description = role.getDescription();
        createdDate = role.getCreatedDate();
        createdBy = role.getCreatedBy();
        updatedDate = role.getUpdatedDate();
        updatedBy = role.getUpdatedBy();
    }
}
