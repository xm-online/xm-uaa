package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.audit.AuditEventConverter;
import com.icthh.xm.uaa.domain.PersistentAuditEvent;
import com.icthh.xm.uaa.repository.projection.PrincipalProjection;
import com.icthh.xm.uaa.service.SeparateTransactionExecutor;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * An implementation of Spring Boot's AuditEventRepository.
 */
@RequiredArgsConstructor
@Repository
public class CustomAuditEventRepository implements AuditEventRepository {

    private static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;
    private final AuditEventConverter auditEventConverter;
    private final ApplicationProperties applicationProperties;
    private final SeparateTransactionExecutor separateTransactionExecutor;

    public List<AuditEvent> find(Date after) {
        Iterable<PersistentAuditEvent> persistentAuditEvents =
            persistenceAuditEventRepository.findByAuditEventDateAfter(after.toInstant());
        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    public List<AuditEvent> find(String principal, Date after) {
        Iterable<PersistentAuditEvent> persistentAuditEvents;
        if (principal == null && after == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findAll();
        } else if (after == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipal(principal);
        } else {
            persistentAuditEvents =
                persistenceAuditEventRepository.findByPrincipalAndAuditEventDateAfter(principal, after.toInstant());
        }
        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        Iterable<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type);
        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    public List<PrincipalProjection> findAfter(Instant after, String type) {
        return persistenceAuditEventRepository.findDistinctByAuditEventDateAfterAndAuditEventType(after, type);
    }

    @Override
    public void add(AuditEvent event) {
        if (!AUTHORIZATION_FAILURE.equals(event.getType())
                && !Constants.ANONYMOUS_USER.equals(event.getPrincipal())
                && applicationProperties.isAuditEventsEnabled()) {
            separateTransactionExecutor.doInSeparateTransaction(() -> {
                PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();
                persistentAuditEvent.setPrincipal(event.getPrincipal());
                persistentAuditEvent.setAuditEventType(event.getType());
                persistentAuditEvent.setAuditEventDate(event.getTimestamp());
                persistentAuditEvent.setData(auditEventConverter.convertDataToStrings(event.getData()));
                return persistenceAuditEventRepository.save(persistentAuditEvent);
            });
        }
    }

    public void delete(String principal) {
        persistenceAuditEventRepository.deleteByPrincipal(principal);
    }

    public void deleteAll() {
        persistenceAuditEventRepository.deleteAll();
    }
}
