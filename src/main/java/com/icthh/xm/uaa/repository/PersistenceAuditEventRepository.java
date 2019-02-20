package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.PersistentAuditEvent;
import com.icthh.xm.uaa.repository.projection.PrincipalProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for the PersistentAuditEvent entity.
 */
public interface PersistenceAuditEventRepository extends JpaRepository<PersistentAuditEvent, Long> {

    List<PersistentAuditEvent> findByPrincipal(String principal);

    List<PersistentAuditEvent> findByAuditEventDateAfter(Instant after);

    List<PersistentAuditEvent> findByPrincipalAndAuditEventDateAfter(String principal, Instant after);

    List<PersistentAuditEvent> findByPrincipalAndAuditEventDateAfterAndAuditEventType(String principle, Instant after, String type);

    List<PrincipalProjection> findDistinctByAuditEventDateAfterAndAuditEventType(Instant after, String type);

    Long deleteByPrincipal(String principal);
}
