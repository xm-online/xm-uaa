package com.icthh.xm.uaa.service;

import com.icthh.xm.uaa.service.dto.PermissionContextDto;

import java.util.Map;

public interface PermissionContextService {

    /**
     * This method returns the latest auth permission context by userKey
     * @param userKey   userKey
     * @return          permission context map
     */
    Map<String, PermissionContextDto> getPermissionContext(String userKey);

}
