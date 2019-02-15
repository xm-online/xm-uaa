package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.SocialUserConnection;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for the Social User Connection entity.
 */
public interface SocialUserConnectionRepository extends JpaRepository<SocialUserConnection, Long> {

    void findByProviderUserIdAndAndProviderId(String providerUserId, String providerId);

}
