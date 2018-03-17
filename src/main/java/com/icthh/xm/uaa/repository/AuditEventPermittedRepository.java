package com.icthh.xm.uaa.repository;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.uaa.domain.PersistentAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class AuditEventPermittedRepository extends PermittedRepository {

    public AuditEventPermittedRepository(PermissionCheckService permissionCheckService) {
        super(permissionCheckService);
    }

    /**
     * Find all permitted audit events between dates.
     * @param fromDate the from date
     * @param toDate the to date
     * @param pageable the page info
     * @param privilegeKey the privilege key
     * @return permitted audit events
     */
    public Page<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate,
                                                                     Instant toDate,
                                                                     Pageable pageable,
                                                                     String privilegeKey) {
        String whereCondition = "auditEventDate between :fromDate and :toDate";

        Map<String, Object> conditionParams = new HashMap<>();
        conditionParams.put("fromDate", fromDate);
        conditionParams.put("toDate", toDate);

        return findByCondition(whereCondition, conditionParams, pageable, getType(), privilegeKey);
    }

    private Class<PersistentAuditEvent> getType() {
        return PersistentAuditEvent.class;
    }
}
