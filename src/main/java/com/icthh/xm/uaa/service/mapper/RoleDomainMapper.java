package com.icthh.xm.uaa.service.mapper;

import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.uaa.domain.RoleEntity;
import lombok.experimental.UtilityClass;

/**
 * Mapper for Role
 */
@UtilityClass
public class RoleDomainMapper {

    public static Role entityToRole(RoleEntity entity) {
        Role result = new Role();
        result.setKey(entity.getRoleKey());
        result.setDescription(entity.getDescription());
        result.setCreatedBy(entity.getCreatedBy());
        result.setCreatedDate(entity.getCreatedDate().toString());
        result.setUpdatedBy(entity.getLastModifiedBy());
        result.setUpdatedDate(entity.getLastModifiedDate().toString());
        return result;
    }

    public static RoleEntity roleToEntity(Role role, RoleEntity roleEntity, String key) {
        roleEntity.setRoleKey(key);
        roleEntity.setDescription(role.getDescription());
        return roleEntity;
    }
}
