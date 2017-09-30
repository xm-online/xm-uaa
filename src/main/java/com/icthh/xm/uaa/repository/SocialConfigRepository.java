package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.SocialConfig;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialConfigRepository {

    Optional<SocialConfig> findOneByProviderIdAndDomain(String providerId, String domain);

    List<SocialConfig> findByDomain(String domain);

}
