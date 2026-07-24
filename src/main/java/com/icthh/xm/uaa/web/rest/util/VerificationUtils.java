package com.icthh.xm.uaa.web.rest.util;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.constant.ErrorConstants;
import java.util.List;
import lombok.experimental.UtilityClass;

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
}
