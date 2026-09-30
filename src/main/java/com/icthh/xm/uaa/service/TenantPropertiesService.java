package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Security;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.icthh.xm.uaa.security.DefaultClientSecretPolicy.validate;

@Slf4j
@Service
@IgnoreLogginAspect
public class TenantPropertiesService extends AbstractTenantPropertiesService<TenantProperties> {

    public TenantPropertiesService(ApplicationProperties applicationProperties,
                                   TenantConfigRepository tenantConfigRepository,
                                   TenantContextHolder tenantContextHolder) {
        super(TenantProperties.class, applicationProperties, tenantConfigRepository, tenantContextHolder);
    }

    @Override
    protected void onTenantPropsUpdated(String tenant, TenantProperties tenantProperties) {
        Security security = tenantProperties.getSecurity();
        validate(tenant, security == null ? null : security.getDefaultClientSecret());
    }
}
