package com.icthh.xm.uaa.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.LepConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class,
    LepConfiguration.class
})
public class SchedulerServiceIntTest {

    private static final String LEP_PATH = "/config/tenants/XM/uaa/lep/scheduler"
                                           + "/SchedulerEvent$$TEST_TYPE_KEY$$around.groovy";

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private SchedulerHandler schedulerHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        leps.onRefresh(LEP_PATH, loadFile(LEP_PATH));
    }

    @After
    public void destroy() {
        leps.onRefresh(LEP_PATH, null);
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    @SneakyThrows
    public void testCallLepOnEvent() {
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data = new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "XM");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(1);
    }

    @Test
    public void testOtherTenantOnEvent() {
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data = new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "DEMO");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(0);
        assertThat(scheduledEvent.getData().get("scheduledEvent")).isNull();
    }

    @Test
    public void testOtherTypeKeyOnEvent() {
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("OTHER_TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data = new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "XM");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(0);
        assertThat(scheduledEvent.getData().get("scheduledEvent")).isNull();
    }

}
