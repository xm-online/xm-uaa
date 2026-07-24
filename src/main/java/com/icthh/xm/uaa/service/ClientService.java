package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.service.RoleService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.ClientState;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import com.icthh.xm.uaa.service.query.ClientQueryService;
import com.icthh.xm.uaa.service.query.filter.StrictClientFilterQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_DESCRIPTION_TOO_LONG;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_DESCRIPTION_TOO_LONG_MESSAGE;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_ROLE_NOT_ALLOWED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.VALIDATION_ROLE_NOT_ALLOWED_MESSAGE;
import static java.util.stream.Collectors.toList;

/**
 * Service Implementation for managing Client.
 */
@LepService(group = "service.client")
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermittedRepository permittedRepository;
    private final ClientQueryService clientQueryService;
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final RoleService roleService;

    public static final String PSWRD_MASK = "*****";

    /**
     * Save a client.
     *
     * @param client the entity to save
     * @return the persisted entity
     */
    public Client save(Client client) {
        return clientRepository.save(client);
    }

    /**
     * Create a client.
     *
     * @param client the entity to create
     * @return the persisted entity
     */
    @LogicExtensionPoint("CreateClient")
    public Client createClient(ClientDTO client) {
        validateClient(client);

        if (getClient(client.getClientId()) != null) {
            throw new BusinessException("client.already.exists",
                                        "Client with client id: " + client.getClientId() + " already exists");
        }

        Client newClient = new Client();
        newClient.setClientId(client.getClientId());
        String clientSecret;
        if (client.getClientSecret() == null) {
            clientSecret = passwordEncoder.encode(StringUtils.EMPTY);
        } else {
            clientSecret = passwordEncoder.encode(client.getClientSecret());
        }
        newClient.setClientSecret(clientSecret);
        newClient.setRoleKey(client.getRoleKey());
        newClient.setDescription(client.getDescription());
        newClient.setAccessTokenValiditySeconds(client.getAccessTokenValiditySeconds());
        newClient.setRefreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds());
        newClient.setScopes(client.getScopes());
        return clientRepository.save(newClient);
    }

    /**
     * Update a client.
     *
     * @param updatedClient the entity to update
     * @return the persisted entity
     */
    @LogicExtensionPoint("UpdateClient")
    public Client updateClient(ClientDTO updatedClient) {
        validateClient(updatedClient);

        return clientRepository.findById(updatedClient.getId()).map(client -> {
            String newClientSecret = updatedClient.getClientSecret();
            if (!PSWRD_MASK.equals(newClientSecret)) {
                if (newClientSecret == null) {
                    newClientSecret = StringUtils.EMPTY;
                }
                client.setClientSecret(passwordEncoder.encode(newClientSecret));
            }
            client.setRoleKey(updatedClient.getRoleKey());
            client.setDescription(updatedClient.getDescription());
            client.setAccessTokenValiditySeconds(updatedClient.getAccessTokenValiditySeconds());
            client.setRefreshTokenValiditySeconds(updatedClient.getRefreshTokenValiditySeconds());
            client.setScopes(updatedClient.getScopes());
            return client;
        }).orElseThrow(() -> new EntityNotFoundException("Entity not found"));
    }

    /**
     * Get all the clients.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("CLIENT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the clients")
    public Page<ClientDTO> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, Client.class, privilegeKey).map(
            source -> new ClientDTO(source.clientSecret(PSWRD_MASK)));
    }

    /**
     * Get all the clients by filter.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindAllFiltered")
    public Page<ClientDTO> findAllFiltered(StrictClientFilterQuery query, Pageable pageable) {
        return clientQueryService.findAllByStrictMatch(query, pageable);
    }

    /**
     * Get one client by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<Client> findOne(Long id) {
        Optional<Client> result = clientRepository.findById(id);
        result.ifPresent(client -> client.setClientSecret(PSWRD_MASK));
        return result;
    }

    /**
     * Delete the  client by id.
     *
     * @param id the id of the entity
     */
    @LogicExtensionPoint("DeleteClient")
    public void delete(Long id) {
        clientRepository.deleteById(id);
    }

    @IgnoreLogginAspect
    public Client getClient(String clientId) {
        return clientRepository.findOneByClientId(clientId);
    }

    @LogicExtensionPoint("FindAllByClientIdContains")
    @Transactional(readOnly = true)
    public Page<ClientDTO> findAllByClientIdContains(String clientId, Pageable pageable) {
        return clientRepository.findAllByClientIdContainingIgnoreCase(clientId, pageable)
              .map(client -> new ClientDTO(client.clientSecret(PSWRD_MASK)));
    }

    @LogicExtensionPoint("FindAllByClientIdIn")
    @Transactional(readOnly = true)
    public List<ClientDTO> findAllByClientIdIn(List<String> clientIds) {
        return clientRepository.findAllByClientIdIn(clientIds).stream()
              .map(client -> new ClientDTO(client.clientSecret(PSWRD_MASK)))
              .collect(toList());
    }

    public Optional<ClientDTO> blockClient(String clientKey) {
        return changeClientState(clientKey, ClientState.BLOCKED);
    }

    public Optional<ClientDTO> activateClient(String clientKey) {
        return changeClientState(clientKey, ClientState.ACTIVE);
    }

    private Optional<ClientDTO> changeClientState(String clientKey, ClientState clientState){
        return Optional.ofNullable(getClient(clientKey))
            .map(client-> {
                client.setState(clientState);
                return client;
            })
            .map(ClientDTO::new);
    }

    private void validateClient(ClientDTO client) {
        if (client.getDescription() != null && client.getDescription().length() > 500) {
            throw new BusinessException(VALIDATION_DESCRIPTION_TOO_LONG, VALIDATION_DESCRIPTION_TOO_LONG_MESSAGE);
        }

        Set<String> configuredRoles = roleService.getRoles(tenantContextHolder.getTenantKey()).keySet();
        if (!isRoleAllowed(client.getRoleKey(), configuredRoles)) {
            throw new BusinessException(VALIDATION_ROLE_NOT_ALLOWED, VALIDATION_ROLE_NOT_ALLOWED_MESSAGE);
        }
    }

    private boolean isRoleAllowed(String roleKey, Set<String> configuredRoles) {
        if (roleKey == null) {
            return false;
        }

        return isAllowedSuperAdminRole(roleKey) || configuredRoles.contains(roleKey);
    }

    private boolean isAllowedSuperAdminRole(String roleKey) {
        return roleKey.equals(SUPER_ADMIN) && applicationProperties.isClientAsSuperAdminEnabled();
    }
}
