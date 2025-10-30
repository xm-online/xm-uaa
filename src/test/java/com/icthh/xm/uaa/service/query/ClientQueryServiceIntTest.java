package com.icthh.xm.uaa.service.query;

import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.ClientState;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import com.icthh.xm.uaa.service.query.filter.StrictClientFilterQuery;
import com.icthh.xm.uaa.utils.builder.ClientBuilder;
import io.github.jhipster.service.filter.StringFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientQueryServiceIntTest {

    private static final String UAA_CONFIG_PATH = "/config/tenants/XM/uaa/uaa.yml";

    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

    private static final String SCOPE_1 = "scope_123456";
    private static final String SCOPE_2 = "scope_456789";
    private static final String SCOPE_3 = "scope_789123";

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientQueryService clientQueryService;

    @Autowired
    private TenantPropertiesService tenantPropertiesService;

    @BeforeAll
    public void before() {
        tenantPropertiesService.onInit(UAA_CONFIG_PATH, readConfigFile(UAA_CONFIG_PATH));

        var clients = List.of(
            new ClientBuilder().builder().withState(ClientState.ACTIVE).build(),
            new ClientBuilder().builder().withState(ClientState.BLOCKED).build(),
            new ClientBuilder().builder().withScope(List.of(SCOPE_1, SCOPE_2)).build(),
            new ClientBuilder().builder().withScope(List.of(SCOPE_2)).build(),
            new ClientBuilder().builder().withScope(List.of(SCOPE_1, SCOPE_3)).build(),
            new ClientBuilder().builder().withRole(ROLE_ANONYMOUS).build()
        );
        clientRepository.saveAll(clients);
    }

    @Test
    public void findAllByStrictMatch_roleKey() {
        StrictClientFilterQuery filterQuery = new StrictClientFilterQuery();
        filterQuery.setClientId(new StringFilter().setContains("test-client-"));
        filterQuery.setRoleKey((StringFilter) new StringFilter().setEquals(ROLE_ANONYMOUS));

        Page<ClientDTO> page = clientQueryService.findAllByStrictMatch(filterQuery, PageRequest.of(0, 1));

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllByStrictMatch_stateActive() {
        StrictClientFilterQuery filterQuery = new StrictClientFilterQuery();
        filterQuery.setClientId(new StringFilter().setContains("test-client-"));
        filterQuery.setState((StringFilter) new StringFilter().setEquals(ClientState.ACTIVE.toString()));

        Page<ClientDTO> page = clientQueryService.findAllByStrictMatch(filterQuery, PageRequest.of(0, 1));

        assertEquals(5, page.getTotalElements()); // webapp client state is null
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllByStrictMatch_stateBlocked() {
        StrictClientFilterQuery filterQuery = new StrictClientFilterQuery();
        filterQuery.setClientId(new StringFilter().setContains("test-client-"));
        filterQuery.setState((StringFilter) new StringFilter().setEquals(ClientState.BLOCKED.toString()));

        Page<ClientDTO> page = clientQueryService.findAllByStrictMatch(filterQuery, PageRequest.of(0, 1));

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllByStrictMatch_scope_123() {
        StrictClientFilterQuery filterQuery = new StrictClientFilterQuery();
        filterQuery.setClientId(new StringFilter().setContains("test-client-"));
        filterQuery.setScopes(new StringFilter().setContains(SCOPE_1));

        Page<ClientDTO> page = clientQueryService.findAllByStrictMatch(filterQuery, PageRequest.of(0, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }

    @Test
    public void findAllByStrictMatch_scope_456() {
        StrictClientFilterQuery filterQuery = new StrictClientFilterQuery();
        filterQuery.setClientId(new StringFilter().setContains("test-client-"));
        filterQuery.setScopes(new StringFilter().setContains(SCOPE_3));

        Page<ClientDTO> page = clientQueryService.findAllByStrictMatch(filterQuery, PageRequest.of(0, 1));

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
    }
}
