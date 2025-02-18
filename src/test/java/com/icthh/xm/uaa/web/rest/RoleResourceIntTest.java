package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.utils.FileUtil.getSingleConfigMap;
import static com.icthh.xm.uaa.utils.FileUtil.readConfigFile;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the RoleResource REST controller.
 *
 * @see RoleResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    UaaApp.class,
    XmOverrideConfiguration.class
})
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class RoleResourceIntTest {

    @MockBean
    private TenantConfigRepository tenantConfigRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private RoleResource roleResource;

    @Autowired
    private CommonConfigRepository commonConfigRepository;

    private MockMvc restMvc;

    @Before
    public void setUp() {
        restMvc = MockMvcBuilders.standaloneSetup(roleResource)
                                 .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        when(tenantConfigRepository
            .getConfigFullPath(DEFAULT_TENANT_KEY_VALUE,"/api/config/tenants/{tenantName}/roles.yml"))
            .thenReturn(readConfigFile("/config/tenants/XM/roles.yml"));
        when(tenantConfigRepository
            .getConfigFullPath(DEFAULT_TENANT_KEY_VALUE, "/api/config/tenants/XM/custom-privileges.yml"))
            .thenReturn(readConfigFile("/config/tenants/XM/custom-privileges.yml"));
        when(commonConfigRepository
            .getConfig(isNull(), eq(singletonList("/config/tenants/privileges.yml"))))
            .thenReturn(getSingleConfigMap("/config/tenants/privileges.yml",
                readConfigFile("/config/tenants/privileges.yml")));

    }

    @Test
    @SneakyThrows
    public void testRestGetRoleMatrixWithCustomPrivileges(){
        restMvc.perform(get("/api/roles/matrix"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles", hasSize(3)))
            .andExpect(jsonPath("$.roles", hasItems("ROLE_ADMIN", "ROLE_USER", "SUPER-ADMIN")))
            .andExpect(jsonPath("$.permissions").isArray())
            .andExpect(jsonPath("$.permissions", hasSize(4)))
            .andExpect(jsonPath("$.permissions[*].description",
                                containsInAnyOrder("Privilege to create new attachment",
                                                   "Privilege to delete attachment",
                                                   "Privilege to get custom privilege",
                                                   "Privilege to edit custom privilege")));

    }

    @Test
    @SneakyThrows
    public void testRestGetRoleByKeyWithCustomPrivileges() {
        restMvc.perform(get("/api/roles/SUPER-ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleKey").value("SUPER-ADMIN"))
            .andExpect(jsonPath("$.permissions").isArray())
            .andExpect(jsonPath("$.permissions", hasSize(4)))
            .andExpect(jsonPath("$.permissions[*].roleKey", hasItems("SUPER-ADMIN")))
            .andExpect(jsonPath("$.permissions[*].description",
                                containsInAnyOrder("Privilege to create new attachment",
                                                   "Privilege to delete attachment",
                                                   "Privilege to get custom privilege",
                                                   "Privilege to edit custom privilege")));

    }

}
