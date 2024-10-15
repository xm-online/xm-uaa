package com.icthh.xm.uaa.security.oauth2.otp;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public abstract class AbstractOtpSender implements OtpSender {

    private final TenantPropertiesService tenantPropertiesService;
    private final ApplicationProperties appProperties;
    private final TenantContextHolder tenantContextHolder;
    private final UserService userService;
    private final XmRequestContextHolder xmRequestContextHolder;

    protected boolean isCommunicationEnabled(TenantKey tenantKey) {
        return Optional.ofNullable(tenantPropertiesService.getTenantProps(tenantKey))
            .map(TenantProperties::getCommunication)
            .map(TenantProperties.Communication::getEnabled)
            .map(Boolean.TRUE::equals)
            .orElse(appProperties.getCommunication() != null && appProperties.getCommunication().isEnabled());
    }

    protected TenantKey getTenantKey() {
        return TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
    }

    protected User getUserByUserKey(String userKey, String tenantKey) {
        return Optional.ofNullable(userService.getUser(userKey))
            .orElseThrow(() -> new IllegalStateException(
                String.format("User by key '%s' not found in tenant: %s", userKey, tenantKey)));
    }

    protected Map<String, Object> getObjectModel(String otp, User user, TenantKey tenantKey) {
        String applicationUrl = UaaUtils.getApplicationUrl(xmRequestContextHolder);

        Map<String, Object> dataBind = new HashMap<>();
        dataBind.put("otp", otp);
        dataBind.put("user", new UserDTO(user));
        dataBind.put("tenant", tenantKey.getValue());
        dataBind.put("appBaseUrl", applicationUrl);
        return dataBind;
    }
}
