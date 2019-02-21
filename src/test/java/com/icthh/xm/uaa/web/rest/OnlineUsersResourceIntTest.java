package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.repository.CustomAuditEventRepository;
import com.icthh.xm.uaa.service.OnlineUsersService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.springframework.boot.actuate.security.AuthenticationAuditListener.AUTHENTICATION_SUCCESS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the ClientResource REST controller.
 *
 * @see ClientResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, XmOverrideConfiguration.class})
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class OnlineUsersResourceIntTest {

    @Autowired
    private CustomAuditEventRepository auditEventRepository;

    @Autowired
    private OnlineUsersService onlineUsersService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc restClientMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.restClientMockMvc = MockMvcBuilders.standaloneSetup(new OnlineUsersResource(onlineUsersService))
            .setControllerAdvice(exceptionTranslator).build();
        TenantContextUtils.setTenant(tenantContextHolder, "XM");
    }

    @Test
    public void getAllOnlineUsers() throws Exception {
        // Initialize the database
        auditEventRepository.add(new AuditEvent("user1", AUTHENTICATION_SUCCESS));
        auditEventRepository.add(new AuditEvent(
            Instant.now().minus(1, ChronoUnit.DAYS),
            "user2",
            AUTHENTICATION_SUCCESS,
            Collections
                .emptyMap()));

        // Get all the clientList
        restClientMockMvc.perform(get("/api/onlineUsers"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andReturn().getResponse().equals(1);
    }
}
