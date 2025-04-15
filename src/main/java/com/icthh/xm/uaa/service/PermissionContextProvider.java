package com.icthh.xm.uaa.service;

import java.util.Map;

/**
 * This interface is used to manage user auth permissions context
 */
public interface PermissionContextProvider {

    /**
     * Get user auth permission context by userKey
     * @param userKey   userKey
     * @return          permission context
     */
    Map<String, Object> getPermissionContext(String userKey);

}
