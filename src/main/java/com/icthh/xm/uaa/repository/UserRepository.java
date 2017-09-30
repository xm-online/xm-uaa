package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByUserKey(String userKey);

    Optional<User> findOneByActivationKey(String activationKey);

    Optional<User> findOneByResetKey(String resetKey);

    @EntityGraph(attributePaths = "authorities")
    User findOneWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = "logins")
    Optional<User> findOneWithLoginsByUserKey(String userKey);

    @EntityGraph(attributePaths = {"logins", "authorities"})
    Optional<User> findOneWithAuthoritiesAndLoginsByUserKey(String userKey);

    @Override
    @EntityGraph(attributePaths = {"logins", "authorities"})
    Page<User> findAll(Pageable pageable);

}
