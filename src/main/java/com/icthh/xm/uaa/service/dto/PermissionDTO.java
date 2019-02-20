package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.commons.permission.domain.Permission;
import lombok.Data;

import java.util.Comparator;
import java.util.Set;

@Data
public class PermissionDTO implements Comparable<PermissionDTO> {

    private String msName;
    private String roleKey;
    private String privilegeKey;
    private boolean enabled;
    private String reactionStrategy;
    private String envCondition;
    private String resourceCondition;
    private Set<String> resources;
    private PermissionType permissionType;

    public PermissionDTO() {
    }

    public PermissionDTO(Permission permission) {
        msName = permission.getMsName();
        roleKey = permission.getRoleKey();
        privilegeKey = permission.getPrivilegeKey();
        enabled = !permission.isDisabled();
        reactionStrategy = permission.getReactionStrategy() == null ? null : permission.getReactionStrategy().name();
        envCondition = permission.getEnvCondition() != null ? permission.getEnvCondition().getExpressionString() : null;
        resourceCondition = permission.getResourceCondition() != null ? permission.getResourceCondition().getExpressionString() : null;
    }

    @Override
    public int compareTo(PermissionDTO o) {
        return Comparator.comparing(PermissionDTO::getMsName)
            .thenComparing(PermissionDTO::getRoleKey)
            .thenComparing(PermissionDTO::getPrivilegeKey)
            .compare(this, o);
    }
}
