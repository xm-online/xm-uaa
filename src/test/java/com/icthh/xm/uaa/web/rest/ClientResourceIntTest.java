package com.icthh.xm.uaa.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.exceptions.spring.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.UserAuthPasswordEncoderConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * Test class for the ClientResource REST controller.
 *
 * @see ClientResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class,
    UserAuthPasswordEncoderConfiguration.class
})
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class ClientResourceIntTest {

    private static final Long DEFAULT_ID = 1L;

    private static final String DEFAULT_CLIENT_ID = "AAAAAAAAAA";
    private static final String UPDATED_CLIENT_ID = "BBBBBBBBBB";

    private static final String DEFAULT_CLIENT_SECRET = "AAAAAAAAAA";
    private static final String UPDATED_CLIENT_SECRET = "BBBBBBBBBB";

    private static final String DEFAULT_ROLE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_ROLE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);

    private static final Instant DEFAULT_UPDATED_DATE = Instant.ofEpochMilli(0L);

    @Autowired
    private ClientResource clientResource;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    @Qualifier("passwordEncoder")
    private PasswordEncoder encoder;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc restClientMockMvc;

    private Client client;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.restClientMockMvc = MockMvcBuilders.standaloneSetup(new ClientResource(clientService, clientResource))
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    /**
     * Create an entity for this test.
     *
     * <p>This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Client createEntity(EntityManager em) {
        Client client =  new Client()
            .clientId(DEFAULT_CLIENT_ID)
            .clientSecret(DEFAULT_CLIENT_SECRET)
            .roleKey(DEFAULT_ROLE_KEY)
            .description(DEFAULT_DESCRIPTION);
        return client;
    }

    @Before
    public void initTest() {
        client = createEntity(em);
    }

    @Test
    public void testUserDTOtoUser() {
        Client client = new Client().clientId(DEFAULT_CLIENT_ID)
            .roleKey(DEFAULT_ROLE_KEY)
            .description(DEFAULT_DESCRIPTION);
        client.setId(DEFAULT_ID);
        client.setCreatedBy(null);
        client.setCreatedDate(DEFAULT_CREATED_DATE);
        client.setLastModifiedBy(null);
        client.setLastModifiedDate(DEFAULT_UPDATED_DATE);

        ClientDTO dto = new ClientDTO(client);

        assertThat(dto.getId()).isEqualTo(DEFAULT_ID);
        assertThat(dto.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(dto.getRoleKey()).isEqualTo(DEFAULT_ROLE_KEY);
        assertThat(dto.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(dto.getCreatedBy()).isNull();
        assertThat(dto.getCreatedDate()).isNotNull();
        assertThat(dto.getLastModifiedBy()).isNull();
        assertThat(dto.getLastModifiedDate()).isNotNull();
    }

    @Test
    public void testUserDTOWithSecretToUser() {
        Client client = new Client().clientId(DEFAULT_CLIENT_ID)
            .clientSecret(DEFAULT_CLIENT_SECRET).roleKey(DEFAULT_ROLE_KEY)
            .description(DEFAULT_DESCRIPTION);
        client.setId(DEFAULT_ID);
        client.setCreatedBy(null);
        client.setCreatedDate(DEFAULT_CREATED_DATE);
        client.setLastModifiedBy(null);
        client.setLastModifiedDate(DEFAULT_UPDATED_DATE);

        ClientDTO dto = new ClientDTO(client);

        assertThat(dto.getId()).isEqualTo(DEFAULT_ID);
        assertThat(dto.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(dto.getClientSecret()).isEqualTo(DEFAULT_CLIENT_SECRET);
        assertThat(dto.getRoleKey()).isEqualTo(DEFAULT_ROLE_KEY);
        assertThat(dto.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(dto.getCreatedBy()).isNull();
        assertThat(dto.getCreatedDate()).isNotNull();
        assertThat(dto.getLastModifiedBy()).isNull();
        assertThat(dto.getLastModifiedDate()).isNotNull();
    }

    @Test
    @Transactional
    public void createClient() throws Exception {
        int databaseSizeBeforeCreate = clientRepository.findAll().size();

        // Create the Client
        restClientMockMvc.perform(post("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(client)))
            .andExpect(status().isCreated());

        // Validate the Client in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeCreate + 1);
        Client testClient = clientList.get(clientList.size() - 1);
        assertThat(testClient.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(encoder.matches(DEFAULT_CLIENT_SECRET, testClient.getClientSecret())).isTrue();
        assertThat(testClient.getRoleKey()).isEqualTo(DEFAULT_ROLE_KEY);
        assertThat(testClient.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    @Transactional
    public void createClientWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = clientRepository.findAll().size();

        // Create the Client with an existing ID
        client.setId(DEFAULT_ID);

        // An entity with an existing ID cannot be created, so this API call must fail
        restClientMockMvc.perform(post("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(client)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllClients() throws Exception {
        // Initialize the database
        clientRepository.saveAndFlush(client);

        // Get all the clientList
        restClientMockMvc.perform(get("/api/clients?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(client.getId().intValue())))
            .andExpect(jsonPath("$.[*].clientId").value(hasItem(DEFAULT_CLIENT_ID)))
            .andExpect(jsonPath("$.[*].roleKey").value(hasItem(DEFAULT_ROLE_KEY)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)));
    }

    @Test
    @Transactional
    public void getClient() throws Exception {
        // Initialize the database
        clientRepository.saveAndFlush(client);

        // Get the client
        restClientMockMvc.perform(get("/api/clients/{id}", client.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(client.getId().intValue()))
            .andExpect(jsonPath("$.clientId").value(DEFAULT_CLIENT_ID))
            .andExpect(jsonPath("$.roleKey").value(DEFAULT_ROLE_KEY))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION));
    }

    @Test
    @Transactional
    public void getNonExistingClient() throws Exception {
        // Get the client
        restClientMockMvc.perform(get("/api/clients/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateClient() throws Exception {
        // Initialize the database
        clientService.save(this.client);

        int databaseSizeBeforeUpdate = clientRepository.findAll().size();

        // Update the client
        ClientDTO updatedClient = new ClientDTO();
        updatedClient.setId(this.client.getId());
        updatedClient.setClientId(UPDATED_CLIENT_ID);
        updatedClient.setClientSecret(UPDATED_CLIENT_SECRET);
        updatedClient.setRoleKey(UPDATED_ROLE_KEY);
        updatedClient.setDescription(UPDATED_DESCRIPTION);

        restClientMockMvc.perform(put("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedClient)))
            .andExpect(status().isOk());

        // Validate the Client in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeUpdate);
        Client testClient = clientList.get(clientList.size() - 1);
        assertThat(testClient.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(encoder.matches(UPDATED_CLIENT_SECRET, testClient.getClientSecret())).isTrue();
        assertThat(testClient.getRoleKey()).isEqualTo(UPDATED_ROLE_KEY);
        assertThat(testClient.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateClientWithEmptyClientSecret() throws Exception {
        // Initialize the database
        clientService.save(this.client);

        int databaseSizeBeforeUpdate = clientRepository.findAll().size();

        // Update the client
        ClientDTO updatedClient = new ClientDTO();
        updatedClient.setId(this.client.getId());
        updatedClient.setClientId(UPDATED_CLIENT_ID);
        updatedClient.setClientSecret(null);
        updatedClient.setRoleKey(UPDATED_ROLE_KEY);
        updatedClient.setDescription(UPDATED_DESCRIPTION);

        restClientMockMvc.perform(put("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedClient)))
            .andExpect(status().isOk());

        // Validate the Client in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeUpdate);
        Client testClient = clientList.get(clientList.size() - 1);
        assertThat(testClient.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(encoder.matches(StringUtils.EMPTY, testClient.getClientSecret())).isTrue();
        assertThat(testClient.getRoleKey()).isEqualTo(UPDATED_ROLE_KEY);
        assertThat(testClient.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateClientIgnoresClientSecret() throws Exception {
        // Initialize the database
        clientService.save(this.client);

        int databaseSizeBeforeUpdate = clientRepository.findAll().size();

        // Update the client
        ClientDTO updatedClient = new ClientDTO();
        updatedClient.setId(this.client.getId());
        updatedClient.setClientId(UPDATED_CLIENT_ID);
        updatedClient.setClientSecret("*****");
        updatedClient.setRoleKey(UPDATED_ROLE_KEY);
        updatedClient.setDescription(UPDATED_DESCRIPTION);

        restClientMockMvc.perform(put("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedClient)))
            .andExpect(status().isOk());

        // Validate the Client in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeUpdate);
        Client testClient = clientList.get(clientList.size() - 1);
        assertThat(testClient.getClientId()).isEqualTo(DEFAULT_CLIENT_ID);
        assertThat(testClient.getClientSecret()).isEqualTo(DEFAULT_CLIENT_SECRET);
        assertThat(testClient.getRoleKey()).isEqualTo(UPDATED_ROLE_KEY);
        assertThat(testClient.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Transactional
    public void updateNonExistingClient() throws Exception {
        int databaseSizeBeforeUpdate = clientRepository.findAll().size();

        // Create the Client

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restClientMockMvc.perform(put("/api/clients")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(client)))
            .andExpect(status().isCreated());

        // Validate the Client in the database
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteClient() throws Exception {
        // Initialize the database
        clientService.save(client);

        int databaseSizeBeforeDelete = clientRepository.findAll().size();

        // Get the client
        restClientMockMvc.perform(delete("/api/clients/{id}", client.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Client> clientList = clientRepository.findAll();
        assertThat(clientList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
