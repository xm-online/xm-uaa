package com.icthh.xm.uaa.repository;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.audit.AuditEventConverter;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.PersistentAuditEvent;
import com.icthh.xm.uaa.service.SeparateTransactionExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test class for the CustomAuditEventRepository customAuditEventRepository class.
 *
 * @see CustomAuditEventRepository
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class CustomAuditEventRepositoryIntTest {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private AuditEventConverter auditEventConverter;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    @Mock
    private ApplicationProperties applicationProperties;

    private CustomAuditEventRepository customAuditEventRepository;

    private PersistentAuditEvent testUserEvent;

    private PersistentAuditEvent testOtherUserEvent;

    private PersistentAuditEvent testOldUserEvent;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);
    }

    @Before
    public void setup() {
        when(applicationProperties.isAuditEventsEnabled()).thenReturn(true);
        customAuditEventRepository = new CustomAuditEventRepository(persistenceAuditEventRepository,
                                                                    auditEventConverter,
                                                                    applicationProperties,
                                                                    separateTransactionExecutor);

        persistenceAuditEventRepository.deleteAll();
        Instant oneHourAgo = Instant.now().minusSeconds(3600);

        testUserEvent = new PersistentAuditEvent();
        testUserEvent.setPrincipal("test-user");
        testUserEvent.setAuditEventType("test-type");
        testUserEvent.setAuditEventDate(oneHourAgo);
        Map<String, String> data = new HashMap<>();
        data.put("test-key", "test-value");
        testUserEvent.setData(data);

        testOldUserEvent = new PersistentAuditEvent();
        testOldUserEvent.setPrincipal("test-user");
        testOldUserEvent.setAuditEventType("test-type");
        testOldUserEvent.setAuditEventDate(oneHourAgo.minusSeconds(10000));

        testOtherUserEvent = new PersistentAuditEvent();
        testOtherUserEvent.setPrincipal("other-test-user");
        testOtherUserEvent.setAuditEventType("test-type");
        testOtherUserEvent.setAuditEventDate(oneHourAgo);
    }

    @AfterTransaction
    public void afterTransaction() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testFindAfter() {
        persistenceAuditEventRepository.save(testUserEvent);
        persistenceAuditEventRepository.save(testOldUserEvent);

        List<AuditEvent> events =
            customAuditEventRepository.find(Date.from(testUserEvent.getAuditEventDate().minusSeconds(3600)));
        assertThat(events).hasSize(1);
        AuditEvent event = events.get(0);
        assertThat(event.getPrincipal()).isEqualTo(testUserEvent.getPrincipal());
        assertThat(event.getType()).isEqualTo(testUserEvent.getAuditEventType());
        assertThat(event.getData()).containsKey("test-key");
        assertThat(event.getData().get("test-key").toString()).isEqualTo("test-value");
        assertThat(event.getTimestamp()).isEqualTo(testUserEvent.getAuditEventDate());
    }

    @Test
    public void testFindByPrincipal() {
        persistenceAuditEventRepository.save(testUserEvent);
        persistenceAuditEventRepository.save(testOldUserEvent);
        persistenceAuditEventRepository.save(testOtherUserEvent);

        List<AuditEvent> events = customAuditEventRepository
            .find("test-user", Date.from(testUserEvent.getAuditEventDate().minusSeconds(3600)));
        assertThat(events).hasSize(1);
        AuditEvent event = events.get(0);
        assertThat(event.getPrincipal()).isEqualTo(testUserEvent.getPrincipal());
        assertThat(event.getType()).isEqualTo(testUserEvent.getAuditEventType());
        assertThat(event.getData()).containsKey("test-key");
        assertThat(event.getData().get("test-key").toString()).isEqualTo("test-value");
        assertThat(event.getTimestamp()).isEqualTo(testUserEvent.getAuditEventDate());
    }

    @Test
    public void testFindByPrincipalNotNullAndAfterIsNull() {
        persistenceAuditEventRepository.save(testUserEvent);
        persistenceAuditEventRepository.save(testOtherUserEvent);

        List<AuditEvent> events = customAuditEventRepository.find("test-user", null);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPrincipal()).isEqualTo("test-user");
    }

    @Test
    public void testFindByPrincipalIsNullAndAfterIsNull() {
        persistenceAuditEventRepository.save(testUserEvent);
        persistenceAuditEventRepository.save(testOtherUserEvent);

        List<AuditEvent> events = customAuditEventRepository.find(null, null);
        assertThat(events).hasSize(2);
        assertThat(events).extracting("principal")
            .containsExactlyInAnyOrder("test-user", "other-test-user");
    }

    @Test
    public void findByPrincipalAndType() {
        persistenceAuditEventRepository.save(testUserEvent);
        persistenceAuditEventRepository.save(testOldUserEvent);

        testOtherUserEvent.setAuditEventType(testUserEvent.getAuditEventType());
        persistenceAuditEventRepository.save(testOtherUserEvent);

        PersistentAuditEvent testUserOtherTypeEvent = new PersistentAuditEvent();
        testUserOtherTypeEvent.setPrincipal(testUserEvent.getPrincipal());
        testUserOtherTypeEvent.setAuditEventType("test-other-type");
        testUserOtherTypeEvent.setAuditEventDate(testUserEvent.getAuditEventDate());
        persistenceAuditEventRepository.save(testUserOtherTypeEvent);

        List<AuditEvent> events = customAuditEventRepository.find("test-user",
            testUserEvent.getAuditEventDate().minusSeconds(3600),
            "test-type");
        assertThat(events).hasSize(1);
        AuditEvent event = events.get(0);
        assertThat(event.getPrincipal()).isEqualTo(testUserEvent.getPrincipal());
        assertThat(event.getType()).isEqualTo(testUserEvent.getAuditEventType());
        assertThat(event.getData()).containsKey("test-key");
        assertThat(event.getData().get("test-key").toString()).isEqualTo("test-value");
        assertThat(event.getTimestamp()).isEqualTo(testUserEvent.getAuditEventDate());
    }

    @Test
    public void addAuditEventEnabled() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(1);
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(event.getPrincipal());
        assertThat(persistentAuditEvent.getAuditEventType()).isEqualTo(event.getType());
        assertThat(persistentAuditEvent.getData()).containsKey("test-key");
        assertThat(persistentAuditEvent.getData().get("test-key")).isEqualTo("test-value");
        assertThat(persistentAuditEvent.getAuditEventDate()).isEqualTo(event.getTimestamp());
    }

    @Test
    public void addAuditEventDisabled() {
        when(applicationProperties.isAuditEventsEnabled()).thenReturn(false);
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(0);
    }

    @Test
    public void testAddEventWithWebAuthenticationDetails() {
        HttpSession session = new MockHttpSession(null, "test-session-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setRemoteAddr("1.2.3.4");
        WebAuthenticationDetails details = new WebAuthenticationDetails(request);
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", details);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(1);
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertThat(persistentAuditEvent.getData().get("remoteAddress")).isEqualTo("1.2.3.4");
        assertThat(persistentAuditEvent.getData().get("sessionId")).isEqualTo("test-session-id");
    }

    @Test
    public void testAddEventWithNullData() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", null);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(1);
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertThat(persistentAuditEvent.getData().get("test-key")).isEqualTo("null");
    }

    @Test
    public void addAuditEventWithAnonymousUser() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent(Constants.ANONYMOUS_USER, "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(0);
    }

    @Test
    public void addAuditEventWithAuthorizationFailureType() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "AUTHORIZATION_FAILURE", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertThat(persistentAuditEvents).hasSize(0);
    }

}
