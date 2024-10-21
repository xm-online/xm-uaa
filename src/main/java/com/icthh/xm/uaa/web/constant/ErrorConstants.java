package com.icthh.xm.uaa.web.constant;

/**
 * Application error constants.
 */
public class ErrorConstants {

    // TODO - move to commons as these cases are common for different MS
    public static final String ERR_TENANT_NOT_SUPPLIED = "error.tenantNotSupplied";
    public static final String ERR_TENANT_SUSPENDED = "error.tenantSuspended";

    public static final String ERROR_PATTERN = "{%n \"error\": \"%s\",%n \"error_description\": \"%s\"%n}";

    public static final String TENANT_NOT_SUPPLIED = "No tenant supplied";
    public static final String TENANT_IS_SUSPENDED = "Tenant is suspended";

    public static final String ERROR_FORBIDDEN_ROLE = "error.role.forbidden";

    public static final String ERROR_USER_DELETE_HIMSELF = "error.user.delete.himself";
    public static final String ERROR_USER_BLOCK_HIMSELF = "error.user.block.himself";
    public static final String ERROR_USER_ACTIVATES_HIMSELF = "error.user.activate.himself";
    public static final String ERROR_USER_LOGIN_INVALID = "error.user.login.invalid";

    public static final String ERROR_SUPER_ADMIN_FORBIDDEN_OPERATION = "error.super-admin.forbidden-operation";

    // error messages
    public static final String ERROR_USER_LOGIN_INVALID_MESSAGE = "User login type could not be determined by value: ";
}
