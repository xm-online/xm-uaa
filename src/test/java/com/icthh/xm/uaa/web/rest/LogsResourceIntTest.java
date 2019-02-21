package com.icthh.xm.uaa.web.rest;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.icthh.xm.commons.logging.web.rest.LogsResource;
import com.icthh.xm.commons.logging.web.rest.vm.LoggerVm;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the LogsResource REST controller.
 *
 * @see LogsResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
public class LogsResourceIntTest {

    private MockMvc restLogsMockMvc;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT_KEY_VALUE);

        LogsResource logsResource = new LogsResource();
        this.restLogsMockMvc = MockMvcBuilders
            .standaloneSetup(logsResource)
            .build();
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void getAllLogs() throws Exception {
        restLogsMockMvc.perform(get("/management/logs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Test
    public void changeLogs() throws Exception {
        LoggerVm logger = new LoggerVm();
        logger.setLevel("INFO");
        logger.setName("ROOT");

        restLogsMockMvc.perform(put("/management/logs")
                                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                    .content(TestUtil.convertObjectToJsonBytes(logger)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void testLogstashAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_LOGSTASH")).isInstanceOf(AsyncAppender.class);
    }

}
