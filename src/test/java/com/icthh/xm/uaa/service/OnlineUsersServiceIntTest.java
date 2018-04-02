package com.icthh.xm.uaa.service;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.repository.OnlineUsersRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class OnlineUsersServiceIntTest {

    @Autowired
    private OnlineUsersService onlineUsersService;

    @Autowired
    private OnlineUsersRepository onlineUsersRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private static final String DEFAULT_KEY = "TEST_KEY";
    private static final String DEFAULT_VALUE = "TEST_VALUE";
    private static final int DEFAULT_TIME_TO_LIVE = 666;

    @Before
    public void initRepository() {
        TenantContextUtils.setTenant(tenantContextHolder, Constants.SUPER_TENANT);
        onlineUsersRepository.deleteAll();
    }

    @Test
    public void assertThatEntryAdd() {
        onlineUsersService.save(DEFAULT_KEY, DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);

        assertThat(onlineUsersRepository.find(DEFAULT_KEY)).isNotNull();
    }

    @Test
    public void assertThatEntryEvicted() throws InterruptedException {
        long timeToLive = 1;
        onlineUsersService.save(DEFAULT_KEY, DEFAULT_VALUE, timeToLive);
        TimeUnit.SECONDS.sleep(timeToLive);
        assertThat(onlineUsersRepository.find(DEFAULT_KEY)).isEmpty();
    }

    @Test
    public void assertThatEntriesGetForSpecifyTenant() throws InterruptedException {
        onlineUsersRepository.save("DEMO:user1", DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);
        onlineUsersRepository.save("DEMO:user2", DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);
        onlineUsersRepository.save("TEST:user1", DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);
        onlineUsersRepository.save("XM:user1", DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);

        Collection<String> allEntries = onlineUsersService.find();
        assertThat(allEntries).isNotNull();
        assertThat(allEntries).size().isEqualTo(4);

        PrivilegedTenantContext privilegedContext = tenantContextHolder.getPrivilegedContext();
        privilegedContext.execute(buildTenant("DEMO"),
                                  () -> {
                                      String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
                                      assertThat(tenant).isEqualTo("DEMO");

                                      Collection<String> entriesForDemoTenant = onlineUsersService.find();
                                      assertThat(entriesForDemoTenant).isNotNull();
                                      assertThat(entriesForDemoTenant).size().isEqualTo(2);
                                  });
    }

    @Test
    public void assertThatEntriesDeleted() throws InterruptedException {
        assertThat(onlineUsersRepository.findAll()).size().isEqualTo(0);
        onlineUsersRepository.save(DEFAULT_KEY, DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);
        assertThat(onlineUsersRepository.findAll()).size().isEqualTo(1);

        onlineUsersService.delete(DEFAULT_KEY);
        assertThat(onlineUsersRepository.findAll()).size().isEqualTo(0);
    }
}
