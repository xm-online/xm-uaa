package com.icthh.xm.uaa.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.exceptions.spring.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.repository.OnlineUsersRepository;
import com.icthh.xm.uaa.service.OnlineUsersService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the ClientResource REST controller.
 *
 * @see ClientResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UaaApp.class, XmOverrideConfiguration.class})
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class OnlineUsersResourceIntTest {

    private static final int DEFAULT_TIME_TO_LIVE = 666;

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String DEFAULT_VALUE = "AAAAAAAAAA";

    @Autowired
    private OnlineUsersRepository onlineUsersRepository;

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

    @Before
    public void init() {
        onlineUsersRepository.deleteAll();
    }

    @Test
    public void getAllOnlineUsers() throws Exception {
        // Initialize the database
        onlineUsersRepository.save(DEFAULT_KEY, DEFAULT_VALUE, DEFAULT_TIME_TO_LIVE);

        // Get all the clientList
        restClientMockMvc.perform(get("/api/onlineUsers"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andReturn().getResponse().equals(1);
    }
}
