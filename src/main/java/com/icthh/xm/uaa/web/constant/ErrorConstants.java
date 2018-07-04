package com.icthh.xm.uaa.web.constant;

/**
 * Application error constants.
 */
public class ErrorConstants {

    // TODO - move to commons as these cases are common for different MS
    public static final String ERR_TENANT_NOT_SUPPLIED = "error.tenantNotSupplied";
    public static final String ERR_TENANT_SUSPENDED = "error.tenantSuspended";

    public static final String ERROR_PATTERN = "{\n \"error\": \"%s\",\n \"error_description\": \"%s\"\n}";

    public static final String TENANT_NOT_SUPPLIED = "No tenant supplied";
    public static final String TENANT_IS_SUSPENDED = "Tenant is suspended";

    public static final String ERROR_USER_DELETE_HIMSELF = "error.user.delete.himself";

    public static final String ERROR_USER_DELETE_SUPER_ADMIN = "error.user.delete.super-admin";

    public static final String ERROR_USER_CREATE_SUPER_ADMIN = "error.user.create.super-admin";

    public static final String ERROR_USER_UPDATE_SUPER_ADMIN = "error.user.update.super-admin";
}
