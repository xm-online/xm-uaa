package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.UserAuthPasswordEncoderConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test class for the TenantRoleResource REST controller.
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
@Ignore
public class TenantRoleResourceIntTest {

}
