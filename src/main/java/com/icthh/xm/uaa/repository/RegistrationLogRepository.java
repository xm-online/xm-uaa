package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.RegistrationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationLogRepository extends JpaRepository<RegistrationLog, Long> {

    Optional<RegistrationLog> findOneByIpAddress(String ipAddress);

}
