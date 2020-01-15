package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.uaa.config.audit.AuditEventConverter;
import com.icthh.xm.uaa.domain.PersistentAuditEvent;
import com.icthh.xm.uaa.repository.AuditEventPermittedRepository;
import com.icthh.xm.uaa.repository.PersistenceAuditEventRepository;

import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing audit events.
 * <p/>
 * <p>
 * This is the default implementation to support SpringBoot Actuator AuditEventRepository
 * </p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuditEventService {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;
    private final AuditEventConverter auditEventConverter;
    private final AuditEventPermittedRepository permittedRepository;

    @FindWithPermission("AUDIT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the auditEvents")
    public Page<AuditEvent> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, PersistentAuditEvent.class, privilegeKey)
            .map(auditEventConverter::convertToAuditEvent);
    }

    @FindWithPermission("AUDIT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the auditEvents")
    public Page<AuditEvent> findByDates(Instant fromDate, Instant toDate, Pageable pageable, String privilegeKey) {
        return permittedRepository.findAllByAuditEventDateBetween(fromDate, toDate, pageable, privilegeKey)
            .map(auditEventConverter::convertToAuditEvent);
    }

    public Optional<AuditEvent> find(Long id) {
        return persistenceAuditEventRepository.findById(id)
            .map(auditEventConverter::convertToAuditEvent);
    }
}
