package com.icthh.xm.uaa.web.rest.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.constant.ErrorConstants;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VerificationUtils {

    public static void assertNotSuperAdmin(UserDTO user) {
        assertNotSuperAdmin(user.getRoleKey());
    }

    public static void assertNotSuperAdmin(String userKey) {
        if (RoleConstant.SUPER_ADMIN.equals(userKey)) {
            throw new BusinessException(ErrorConstants.ERROR_SUPER_ADMIN_FORBIDDEN_OPERATION, "This operation can not be applied to SUPER-ADMIN");
        }
    }
}
