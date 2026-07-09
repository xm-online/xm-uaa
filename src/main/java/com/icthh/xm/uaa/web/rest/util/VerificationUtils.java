package com.icthh.xm.uaa.web.rest.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.constant.ErrorConstants;
import java.util.List;
import lombok.experimental.UtilityClass;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_ROLE_NOT_ALLOWED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_ROLE_NOT_ALLOWED_MESSAGE;

@UtilityClass
public class VerificationUtils {

    public static void assertNotSuperAdmin(UserDTO user) {
        assertNotSuperAdmin(user.getAuthorities());
    }

    public static void assertNotSuperAdmin(List<String> authorities) {
        if (authorities.contains(RoleConstant.SUPER_ADMIN)) {
            throw new BusinessException(ErrorConstants.ERROR_SUPER_ADMIN_FORBIDDEN_OPERATION, "This operation can not be applied to SUPER-ADMIN");
        }
    }

    public static void assertNotSuperAdmin(ClientDTO client) {
        if (client.getRoleKey() != null && SUPER_ADMIN.equals(client.getRoleKey())) {
            throw new BusinessException(VALIDATION_ROLE_NOT_ALLOWED, VALIDATION_ROLE_NOT_ALLOWED_MESSAGE);
        }
    }
}
