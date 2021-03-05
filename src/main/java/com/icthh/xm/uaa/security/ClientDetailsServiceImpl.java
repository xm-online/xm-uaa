package com.icthh.xm.uaa.security;

import com.google.common.base.Preconditions;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.ClientState;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@LepService(group = "service.client.details")
public class ClientDetailsServiceImpl implements ClientDetailsService {

    private final ClientService clientService;

    private final ApplicationProperties applicationProperties;

    private final PasswordEncoder passwordEncoder;

    private final TenantPropertiesService tenantPropertiesService;

    @LogicExtensionPoint("LoadClientByClientId")
    @Override
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

        if (ClientState.BLOCKED.equals(principal.getState())) {
            throw new ClientRegistrationException("Client " + clientId + " is blocked");
        }

        return new ClientDetailsImpl(principal, applicationProperties.getClientGrantTypes(),
            applicationProperties.getClientScope());
    }
}
