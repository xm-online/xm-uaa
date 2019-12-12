package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.UserLogin;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginRepository extends JpaRepository<UserLogin, Long> {

    Optional<UserLogin> findOneByLoginIgnoreCase(final String login);

    Optional<UserLogin> findOneByLoginIgnoreCaseAndUserIdNot(final String login, final Long id);

    Optional<UserLogin> findOneByLogin(final String login);

    Page<UserLogin> findAllByLoginContainingIgnoreCase(String login, Pageable pageable);
}
