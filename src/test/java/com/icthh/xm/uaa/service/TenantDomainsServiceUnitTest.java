package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.tenant.TenantKey;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.icthh.xm.uaa.service.TenantDomainsService.TENANT_DOMAINS_CONFIG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TenantDomainsServiceUnitTest {

    private static final TenantKey TENANT = TenantKey.valueOf("TEST");

    private TenantDomainsService service;

    @Before
    public void setUp() {
        service = new TenantDomainsService();
    }

    @Test
    public void shouldReturnDomainsOfTenantInLowerCase() {
        service.onInit(TENANT_DOMAINS_CONFIG_KEY, "test:\n  - Test.Example.com\nother:\n  - other.example.com\n");

        assertEquals(Set.of("test.example.com"), service.getDomains(TENANT));
    }

    @Test
    public void shouldReturnEmptyWhenNothingConfigured() {
        service.onInit(TENANT_DOMAINS_CONFIG_KEY, "TEST:\nother:\n  - other.example.com\n");

        assertTrue(service.getDomains(TENANT).isEmpty());
        assertTrue(service.getDomains(TenantKey.valueOf("UNKNOWN")).isEmpty());
    }

    @Test
    public void shouldClearDomainsWhenConfigRemoved() {
        service.onInit(TENANT_DOMAINS_CONFIG_KEY, "TEST:\n  - test.example.com\n");

        service.onRefresh(TENANT_DOMAINS_CONFIG_KEY, "");

        assertTrue(service.getDomains(TENANT).isEmpty());
    }

    @Test
    public void shouldListenOnlyTenantDomainsConfig() {
        assertTrue(service.isListeningConfiguration("/config/tenants/tenant-domains.yml"));
        assertFalse(service.isListeningConfiguration("/config/tenants/TEST/uaa/uaa.yml"));
    }
}
