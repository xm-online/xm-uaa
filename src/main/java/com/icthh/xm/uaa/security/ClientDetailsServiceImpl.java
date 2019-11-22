package com.icthh.xm.uaa.security;

import com.google.common.base.Preconditions;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Component;

@Primary
@Component
@Slf4j
@AllArgsConstructor
public class ClientDetailsServiceImpl implements ClientDetailsService {

    private final ClientService clientService;

    private final ApplicationProperties applicationProperties;

    private final PasswordEncoder passwordEncoder;

    private final TenantPropertiesService tenantPropertiesService;

    @Override
    @Cacheable(cacheManager = "requestScopedCacheManager", cacheNames = "default")
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        log.info("Load client with clientId={}", clientId);
        Preconditions.checkNotNull(clientId);
        Client principal;

        if (applicationProperties.getDefaultClientId().contains(clientId)) {
            principal = new Client();
            principal.setClientId(clientId);

            principal.setClientSecret(passwordEncoder.encode(
                tenantPropertiesService.getTenantProps().getSecurity().getDefaultClientSecret()));

            principal.setRoleKey(RoleConstant.SUPER_ADMIN);
        } else {
            try {
                principal = clientService.getClient(clientId);
            } catch (Exception e) {
                log.error("Exception on getting client", e);
                throw new ClientRegistrationException("Client was not found: " + clientId, e);
            }
        }

        if (principal == null) {
            log.error("Client was not found: {}", clientId);
            throw new ClientRegistrationException("Client was not found: " + clientId);
        }

        return new ClientDetailsImpl(principal, applicationProperties.getClientGrantTypes(),
            applicationProperties.getClientScope());
    }
}
