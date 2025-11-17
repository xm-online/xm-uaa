package com.icthh.xm.uaa.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.uaa.domain.Client;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Client entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, ResourceRepository, JpaSpecificationExecutor<Client> {

    Client findOneByClientId(String clientId);

    @Override
    Page<Client> findAll(Pageable pageable);

    List<Client> findByRoleKey(String roleKey);

    Page<Client> findAllByClientIdContainingIgnoreCase(String clientId, Pageable pageable);

    List<Client> findAllByClientIdIn(List<String> clientIds);

}
