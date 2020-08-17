package com.icthh.xm.uaa.service.account.password.reset;

import com.icthh.xm.uaa.service.account.password.reset.type.custom.CustomPasswordResetHandler;
import com.icthh.xm.uaa.service.account.password.reset.type.email.EmailPasswordResetHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PasswordResetHandlerFactory.class)
@RunWith(SpringRunner.class)
public class PasswordResetHandlerFactoryIntTest {

    @Autowired
    private PasswordResetHandlerFactory passwordResetHandlerFactory;

    @MockBean
    private CustomPasswordResetHandler userRepository;
    @MockBean
    private EmailPasswordResetHandler userLoginRepository;

    @Test
    public void testGetPasswordResetHandlerForPredefinedResetType() {
        //GIVEN
        String resetType = "EMAIL";

        //WHEN
        PasswordResetHandler handler = passwordResetHandlerFactory.getPasswordResetHandler(resetType);

        //THEN
        assertThat(handler).isInstanceOf(EmailPasswordResetHandler.class);
    }

    @Test
    public void testGetPasswordResetHandlerForCustomResetType() {
        //GIVEN
        String resetType = "OTP";

        //WHEN
        PasswordResetHandler handler = passwordResetHandlerFactory.getPasswordResetHandler(resetType);

        //THEN
        assertThat(handler).isInstanceOf(CustomPasswordResetHandler.class);
    }
}
