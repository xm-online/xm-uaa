package com.icthh.xm.uaa.social.connect.web;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.social.connect.web.configurable.ConfigOAuth2ConnectionFactory;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainConnectionFactoryLocator implements ConnectionFactoryLocator {

    private final TenantPropertiesService tenantPropertiesService;

    @Override
    public OAuth2ConnectionFactory<?> getConnectionFactory(String providerId) {
        Social social = tenantPropertiesService.getTenantProps().getSocial()
                                               .stream()
                                               .filter(s -> s.getProviderId().equals(providerId))
                                               .findAny()
                                               .get();
        return new ConfigOAuth2ConnectionFactory(social);
    }

    @Override
    public <A> ConnectionFactory<A> getConnectionFactory(Class<A> apiType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> registeredProviderIds() {
        throw new UnsupportedOperationException();
    }
}
