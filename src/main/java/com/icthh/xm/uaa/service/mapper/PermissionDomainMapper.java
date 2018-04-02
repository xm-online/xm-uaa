package com.icthh.xm.uaa.service.mapper;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.uaa.service.dto.PermissionDTO;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Mapper for the entity {@link com.icthh.xm.commons.permission.domain.Permission} and
 * {@link com.icthh.xm.uaa.service.dto.PermissionDTO}.
 */
@UtilityClass
public class PermissionDomainMapper {

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

        ExpressionParser parser = new SpelExpressionParser();
        if (StringUtils.isNotBlank(permissionDto.getEnvCondition())) {
            try {
                permission.setEnvCondition(parser.parseExpression(permissionDto.getEnvCondition()));
            } catch (ParseException e) {
                throw new BusinessException("Error while parsing environment condition for "
                                + permissionDto.getPrivilegeKey());
            }
        }
        if (StringUtils.isNotBlank(permissionDto.getResourceCondition())) {
            try {
                permission.setResourceCondition(parser.parseExpression(permissionDto.getResourceCondition()));
            } catch (ParseException e) {
                throw new BusinessException("Error while parsing resource condition for "
                    + permissionDto.getPrivilegeKey());
            }
        }
        return permission;
    }
}
