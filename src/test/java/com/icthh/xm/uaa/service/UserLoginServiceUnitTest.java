package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserLoginServiceUnitTest {
    private UserLoginService testedInstance;

    @Mock
    private UserLoginRepository loginRepository;

    @Before
    public void setUp() {
        testedInstance = new UserLoginService(loginRepository);
    }

    @Test
    public void testNormalizeLoginsWithWhitespaces() {
        UserLogin login = new UserLogin();
        login.setLogin("  login   ");
        testedInstance.normalizeLogins(List.of(login));

        assertThat(login.getLogin()).isEqualTo("login");
    }

    @Test
    public void testNormalizeLoginsWithUppercaseCharacters() {
        UserLogin login = new UserLogin();
        login.setLogin("LoGin");
        testedInstance.normalizeLogins(List.of(login));

        assertThat(login.getLogin()).isEqualTo("login");
    }

    @Test(expected = BusinessException.class)
    public void shouldVerifyLoginNotExists() {
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin("login");
        when(loginRepository.findOneByLoginIgnoreCase("login")).thenReturn(Optional.of(userLogin));

        testedInstance.verifyLoginsNotExist(List.of(userLogin));

        // should fail

    }

    @Test
    public void shouldVerifyLoginNotExistsExceptGivenUser() {
        Long userId = 1L;
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin("login");
        when(loginRepository.findOneByLoginIgnoreCaseAndUserIdNot("login", userId)).thenReturn(Optional.empty());

        testedInstance.verifyLoginsNotExist(List.of(userLogin), userId);

        // shouldn't fail
    }

    @Test(expected = BusinessException.class)
    public void shouldThrowExceptionWhenDifferentUserWithSameLoginExists() {
        Long userId = 1L;
        UserLogin userLogin = new UserLogin();
        userLogin.setLogin("login");

        UserLogin existLogin = new UserLogin();
        userLogin.setId(2L);
        userLogin.setLogin("login");
        when(loginRepository.findOneByLoginIgnoreCaseAndUserIdNot("login", userId)).thenReturn(Optional.of(existLogin));

        testedInstance.verifyLoginsNotExist(List.of(userLogin), userId);

        // should fail
    }
}
