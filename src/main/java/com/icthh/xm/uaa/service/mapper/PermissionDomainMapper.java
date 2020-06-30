package com.icthh.xm.uaa.service.mapper;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.uaa.domain.PermissionEntity;
import com.icthh.xm.uaa.domain.RoleEntity;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Optional;

/**
 * Mapper for the entity {@link com.icthh.xm.commons.permission.domain.Permission},
 * {@link com.icthh.xm.uaa.service.dto.PermissionDTO} and {@link com.icthh.xm.uaa.domain.PermissionEntity}.
 */
@UtilityClass
public class PermissionDomainMapper {

    private static ExpressionParser parser = new SpelExpressionParser();

    public static PermissionDTO permissionToPermissionDto(Permission permission) {
        return new PermissionDTO(permission);
    }

    public static Permission permissionDtoToPermission(PermissionDTO permissionDto) {
        Permission permission = new Permission();
        permission.setMsName(permissionDto.getMsName());
        permission.setRoleKey(permissionDto.getRoleKey());
        permission.setPrivilegeKey(permissionDto.getPrivilegeKey());
        permission.setDisabled(!permissionDto.isEnabled());
        permission.setReactionStrategy(StringUtils.isBlank(permissionDto.getReactionStrategy()) ? null :
            ReactionStrategy.valueOf(permissionDto.getReactionStrategy().toUpperCase()));

        permission.setEnvCondition(parseCondition(permissionDto.getEnvCondition(),
            "Error while parsing environment condition for " + permissionDto.getPrivilegeKey()));
        permission.setResourceCondition(parseCondition(permissionDto.getResourceCondition(),
            "Error while parsing resource condition for " + permissionDto.getPrivilegeKey()));

        return permission;
    }

    public static Permission permissionEntityToPermission(PermissionEntity permissionEntity, String roleKey) {
        Permission result = new Permission();
        result.setRoleKey(roleKey);
        result.setPrivilegeKey(permissionEntity.getPrivilegeKey());
        result.setMsName(permissionEntity.getMsName());
        result.setDisabled(permissionEntity.isDisabled());
        result.setEnvCondition(parseCondition(permissionEntity.getEnvCondition(),
            "Error while parsing environment condition for " + permissionEntity.getPrivilegeKey()));
        result.setResourceCondition(parseCondition(permissionEntity.getEnvCondition(),
            "Error while parsing resource condition for " + permissionEntity.getPrivilegeKey()));
        result.setReactionStrategy(permissionEntity.getReactionStrategy());
        return result;
    }

    private static Expression parseCondition(String condition, String errorMessage) {
        if (StringUtils.isNotBlank(condition)) {
            try {
                return parser.parseExpression(condition);
            } catch (ParseException e) {
                throw new BusinessException(errorMessage);
            }
        }
        return null;
    }

    public static PermissionEntity permissionToPermissionEntity(Permission permission, PermissionEntity entity, RoleEntity role, String msName) {
        entity.setPrivilegeKey(permission.getPrivilegeKey());
        entity.setMsName(msName);
        entity.setDisabled(permission.isDisabled());
        entity.setEnvCondition(Optional.ofNullable(permission.getEnvCondition()).map(Expression::getExpressionString).orElse(null));
        entity.setResourceCondition(Optional.ofNullable(permission.getResourceCondition()).map(Expression::getExpressionString).orElse(null));
        entity.setReactionStrategy(permission.getReactionStrategy());

        entity.setRole(role);
        return entity;
    }
}
