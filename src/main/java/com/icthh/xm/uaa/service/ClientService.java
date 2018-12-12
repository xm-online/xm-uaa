package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing Client.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermittedRepository permittedRepository;

    private static final String PSWRD_MASK = "*****";

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
    public Client createClient(ClientDTO client) {
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
        newClient.setScopes(client.getScopes());
        return clientRepository.saveAndFlush(newClient);
    }

    /**
     * Update a client.
     *
     * @param updatedClient the entity to update
     * @return the persisted entity
     */
    public Client updateClient(ClientDTO updatedClient) {
        return Optional.of(clientRepository.findOne(updatedClient.getId())).map(client -> {
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
            client.setScopes(updatedClient.getScopes());
            return client;
        }).orElseThrow(() -> new EntityNotFoundException("Entity not found"));
    }

    /**
     *  Get all the clients.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("CLIENT.GET_LIST")
    public Page<ClientDTO> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, Client.class, privilegeKey).map(
            source -> new ClientDTO(source.clientSecret(PSWRD_MASK)));
    }

    /**
     *  Get one client by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<Client> findOne(Long id) {
        Client result = clientRepository.findOne(id);
        if (result != null) {
            result.setClientSecret(PSWRD_MASK);
        }
        return Optional.ofNullable(result);
    }

    /**
     *  Delete the  client by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        clientRepository.delete(id);
    }

    public Client getClient(String clientId) {
        return clientRepository.findOneByClientId(clientId);
    }
}
