package com.icthh.xm.uaa.service;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.actuate.security.AuthenticationAuditListener.AUTHENTICATION_SUCCESS;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.repository.CustomAuditEventRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class OnlineUsersServiceIntTest {

    @Autowired
    private OnlineUsersService onlineUsersService;
    @Autowired
    private CustomAuditEventRepository auditEventRepository;
    @Autowired
    private TenantContextHolder tenantContextHolder;

    private static final String DEFAULT_KEY = "TEST_KEY";
    private static final String DEFAULT_VALUE = "TEST_VALUE";

    @BeforeTransaction
    public void initRepository() {
        TenantContextUtils.setTenant(tenantContextHolder, Constants.SUPER_TENANT);
        auditEventRepository.deleteAll();
    }

    @Test
    public void assertThatEntryAdd() {
        auditEventRepository.add(new AuditEvent(DEFAULT_KEY, AUTHENTICATION_SUCCESS, Collections.emptyMap()));
        assertThat(onlineUsersService.find()).hasSize(1);
    }

    @Test
    public void assertThatEntryEvicted() throws InterruptedException {
        long timeToLive = 1;
        auditEventRepository.add(new AuditEvent(DEFAULT_KEY, AUTHENTICATION_SUCCESS, Collections.emptyMap()));
        TimeUnit.SECONDS.sleep(timeToLive);
        assertThat(auditEventRepository.findAfter(Instant.now(), DEFAULT_VALUE)).isEmpty();
    }

    @Test
    public void assertThatEntriesGetForSpecifyTenant() {
        PrivilegedTenantContext privilegedContext = tenantContextHolder.getPrivilegedContext();
        privilegedContext.execute(buildTenant("DEMO"),
            () -> {
                auditEventRepository.add(new AuditEvent("DEMO:user1", AUTHENTICATION_SUCCESS));
                auditEventRepository.add(new AuditEvent("DEMO:user2", AUTHENTICATION_SUCCESS));
            });
        privilegedContext.execute(buildTenant(Constants.SUPER_TENANT),
            () -> {
                auditEventRepository.add(new AuditEvent("TEST:user1", AUTHENTICATION_SUCCESS));
                auditEventRepository.add(new AuditEvent("XM:user1", AUTHENTICATION_SUCCESS));
            });
        privilegedContext.execute(buildTenant("DEMO"),
            () -> {
                String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
                assertThat(tenant).isEqualTo("DEMO");

                Collection<String> entriesForDemoTenant = onlineUsersService.find();
                assertThat(entriesForDemoTenant).isNotNull();
                assertThat(entriesForDemoTenant).size().isEqualTo(2);
            });
        privilegedContext.execute(buildTenant(Constants.SUPER_TENANT),
            () -> {
                String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
                assertThat(tenant).isEqualTo(Constants.SUPER_TENANT);

                Collection<String> entriesForDemoTenant = onlineUsersService.find();
                assertThat(entriesForDemoTenant).isNotNull();
                assertThat(entriesForDemoTenant).size().isEqualTo(2);
            });
    }

    @Test
    public void assertThatEntriesDeleted() {
        assertThat(onlineUsersService.find()).size().isEqualTo(0);
        auditEventRepository.add(new AuditEvent(DEFAULT_KEY, AUTHENTICATION_SUCCESS, Collections.emptyMap()));
        assertThat(onlineUsersService.find().size()).isEqualTo(1);

        onlineUsersService.delete(DEFAULT_KEY);
        assertThat(onlineUsersService.find()).size().isEqualTo(0);
    }
}
