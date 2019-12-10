package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.rest.util.HeaderUtil;
import com.icthh.xm.uaa.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing Client.
 */
@RestController
@RequestMapping("/api")
public class ClientResource {

    private static final String ENTITY_NAME = "client";

    private final ClientService clientService;
    private final ClientResource clientResource;

    public ClientResource(
                  ClientService clientService,
                  @Lazy ClientResource clientResource) {
        this.clientService = clientService;
        this.clientResource = clientResource;
    }

    /**
     * POST /clients : Create a new client.
     * @param client the client to create
     * @return the ResponseEntity with status 201 (Created) and with body the new client, or with
     *         status 400 (Bad Request) if the client has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/clients")
    @Timed
    @PreAuthorize("hasPermission({'client': #client}, 'CLIENT.CREATE')")
    public ResponseEntity<Void> createClient(@RequestBody ClientDTO client) throws URISyntaxException {
        if (client.getId() != null) {
            return ResponseEntity
                            .badRequest()
                            .headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists",
                                            "A new client cannot already have an ID")).body(null);
        }
        Client result = clientService.createClient(client);
        return ResponseEntity.created(new URI("/api/clients/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString())).build();
    }

    /**
     * PUT /clients : Updates an existing client.
     * @param client the client to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated client, or with
     *         status 400 (Bad Request) if the client is not valid, or with status 500 (Internal
     *         Server Error) if the client couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/clients")
    @Timed
    @PreAuthorize("hasPermission({'id': #client.id, 'newClient': #client}, 'client', 'CLIENT.UPDATE')")
    public ResponseEntity<Void> updateClient(@RequestBody ClientDTO client) throws URISyntaxException {
        if (client.getId() == null) {
            //in order to call method with permissions check
            return this.clientResource.createClient(client);
        }
        Client result = clientService.updateClient(client);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, result.getId().toString())).build();
    }

    /**
     * GET  /clients : get all the clients.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of clients in body
     */
    @GetMapping("/clients")
    @Timed
    public ResponseEntity<List<ClientDTO>> getAllClients(@ApiParam Pageable pageable) {
        Page<ClientDTO> page = clientService.findAll(pageable, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/clients");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /clients/clientid-contains/:clientId : get the clients.
     *
     * @param clientId part of the clientId of the clients to find
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and body with list of clients, or the empty list
     */
    @GetMapping("/clients/clientid-contains")
    @Timed
    public ResponseEntity<List<ClientDTO>> getAllClientsByClientIdContains(@RequestParam String clientId, Pageable pageable) {
        Page<ClientDTO> page = clientService.findAllByClientIdContains(clientId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/clients/clientid-contains");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /clients/:id : get the "id" client.
     *
     * @param id the id of the client to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the client, or with status 404 (Not Found)
     */
    @GetMapping("/clients/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'CLIENT.GET_LIST.ITEM')")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(clientService.findOne(id).map(ClientDTO::new));
    }

    /**
     * DELETE  /clients/:id : delete the "id" client.
     *
     * @param id the id of the client to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/clients/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'client', 'CLIENT.DELETE')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
