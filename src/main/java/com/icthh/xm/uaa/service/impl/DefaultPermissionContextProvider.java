package com.icthh.xm.uaa.service.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.service.PermissionContextProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of user permission context provider.
 */
@Slf4j
@Transactional
@AllArgsConstructor
@LepService(group = "service.user")
@ConditionalOnProperty(value = "application.permission-context-provider", havingValue = "defaultCtxImpl", matchIfMissing = true)
public class DefaultPermissionContextProvider implements PermissionContextProvider {

    @LogicExtensionPoint("PermissionContext")
    @Override
    public Map<String, Object> getPermissionContext(String userKey) {
        log.info("Default implementation of user permission context provider");
        return new HashMap<>();
    }
}
