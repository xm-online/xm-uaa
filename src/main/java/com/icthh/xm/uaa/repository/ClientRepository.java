package com.icthh.xm.uaa.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.uaa.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Client entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, ResourceRepository {

    Client findOneByClientId(String clientId);

    @Override
    Page<Client> findAll(Pageable pageable);

    List<Client> findByRoleKey(String roleKey);

}
