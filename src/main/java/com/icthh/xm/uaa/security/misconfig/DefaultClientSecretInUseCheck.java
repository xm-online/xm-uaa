package com.icthh.xm.uaa.security.misconfig;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.uaa.security.DefaultClientSecretSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
public class DefaultClientSecretInUseCheck implements MisconfigurationCheck {

    private final TenantListRepository tenantListRepository;
    private final DefaultClientSecretSupplier defaultClientSecret;

    /** Tenants known to use the well-known secret; null until the first full scan. */
    private volatile Set<String> flaggedTenants;

    public DefaultClientSecretInUseCheck(TenantListRepository tenantListRepository,
                                         DefaultClientSecretSupplier defaultClientSecret) {
        this.tenantListRepository = tenantListRepository;
        this.defaultClientSecret = defaultClientSecret;
    }

    @Override
    public Result check() {
        Set<String> toCheck = flaggedTenants == null ? activeTenants() : flaggedTenants;

        Set<String> stillUsingWellKnown = new TreeSet<>();
        for (String tenant : toCheck) {
            if (isWellKnownSecretInUse(tenant)) {
                stillUsingWellKnown.add(tenant);
            }
        }
        flaggedTenants = stillUsingWellKnown;

        if (stillUsingWellKnown.isEmpty()) {
            return Result.ok();
        }
        return Result.misconfigured("!!! MISCONFIGURATION !!!" +
            "Tenant(s) " + stillUsingWellKnown + " use the well-known default "
            + "client secret. Set a unique security.defaultClientSecret in uaa.yml.");
    }

    private boolean isWellKnownSecretInUse(String tenant) {
        try {
            return defaultClientSecret.isWellKnownSecretInUse(tenant);
        } catch (Exception e) {
            log.error("Failed to check if default client secret is in use for tenant '{}'", tenant, e);
            return false;
        }
    }

    private Set<String> activeTenants() {
        return tenantListRepository.getTenants();
    }

}
