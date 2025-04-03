package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.ImpersonateLoginAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpersonateLoginAuditEventRepository extends JpaRepository<ImpersonateLoginAuditEvent, Long> {

}
