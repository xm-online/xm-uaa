package com.icthh.xm.uaa.repository;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.uaa.domain.SocialConfig;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomSocialConfigRepository implements SocialConfigRepository {

    private final TenantPropertiesService tenantPropertiesService;

    @Override
    public Optional<SocialConfig> findOneByProviderIdAndDomain(String providerId, String domain) {
        return tenantPropertiesService.getTenantProps().getSocial().stream()
            .filter(s -> s.getProviderId().equals(providerId) && s.getDomain().equals(domain))
            .map(SocialConfig::new)
            .findFirst();
    }

    @Override
    public List<SocialConfig> findByDomain(String domain) {
        return tenantPropertiesService.getTenantProps().getSocial().stream()
            .filter(s -> s.getDomain().equals(domain))
            .map(SocialConfig::new).collect(toList());
    }

}
