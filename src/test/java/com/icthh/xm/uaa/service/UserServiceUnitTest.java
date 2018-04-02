package com.icthh.xm.uaa.service;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceUnitTest {

    private static final String USER_KEY = "userKey";
    private static final Long ID = 1L;

    @InjectMocks
    private UserService service;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLoginRepository userLoginRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SocialService socialService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void getUser() {
        User user = createUser();
        when(userRepository.findOneByUserKey(USER_KEY)).thenReturn(Optional.ofNullable(user));
        assertEquals(user, service.getUser(USER_KEY));

        verify(userRepository).findOneByUserKey(USER_KEY);
    }

    @Test
    public void saveUser() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);
        service.saveUser(user);

        verify(userRepository).save(user);
    }

    private User createUser() {
        User user = new User();
        user.setId(ID);
        user.setUserKey(USER_KEY);
        return user;
    }
}
