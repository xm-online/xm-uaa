package com.icthh.xm.uaa.service;

import static com.icthh.xm.uaa.config.Constants.SUPER_TENANT;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.repository.OnlineUsersRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Service Implementation for managing online users.
 */
@Service
@Slf4j
@AllArgsConstructor
public class OnlineUsersService {

    private final OnlineUsersRepository hazelcastRepository;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Save online user in repository.
     * @param key key for search
     * @param jti JWT identifier as value in entry
     * @param timeToLive the time in seconds after what the entry will be evicted
     */
    public void save(String key, String jti, long timeToLive) {
        hazelcastRepository.save(key, jti, timeToLive);
    }

    /**
     * Find online users for current tenant. But for super tenant it finds online users from all tenants.
     * @return collection of online users
     */
    public Collection<String> find() {
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        if (SUPER_TENANT.equals(tenant)) {
            return hazelcastRepository.findAll();
        } else {
            return hazelcastRepository.find(tenant + ":%");
        }
    }

    /**
     * Delete online user from repository by key.
     * @param key key for search
     */
    public void delete(String key) {
        hazelcastRepository.delete(key);
    }
}
