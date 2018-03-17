package com.icthh.xm.uaa.service;

import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for managing user's login.
 */
@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UserLoginService {

    private final UserLoginRepository userLoginRepository;

    public Optional<UserLogin> getUserByLogin(String login) {
        return userLoginRepository.findOneByLoginIgnoreCase(login);
    }
}
