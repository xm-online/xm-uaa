package com.icthh.xm.uaa.service.impl;

import com.icthh.xm.uaa.service.PermissionContextProvider;
import com.icthh.xm.uaa.service.PermissionContextAggregator;
import com.icthh.xm.uaa.service.dto.PermissionContextDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Default implementation of user permission context provider.
 */
@Slf4j
@Transactional
@AllArgsConstructor
@Service
@ConditionalOnProperty(value = "application.permission-context-provider", havingValue = "defaultCtxImpl", matchIfMissing = true)
public class DefaultPermissionContextProvider implements PermissionContextProvider {

    private final PermissionContextAggregator permissionContextAggregator;

    @Override
    public Map<String, PermissionContextDto> getPermissionContext(String userKey) {
        log.info("Default implementation of user permission context provider");
        return permissionContextAggregator.loadPermissionsFromServices(userKey);
    }
}
