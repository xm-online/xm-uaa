package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLoginRepository extends JpaRepository<UserLogin, Long> {

    Optional<UserLogin> findOneByLoginIgnoreCase(final String login);

    Optional<UserLogin> findOneByLoginIgnoreCaseAndUserIdNot(final String login, final Long id);

    Optional<UserLogin> findOneByLogin(final String login);
}
