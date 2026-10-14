package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PORT;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PROTOCOL;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_WEB_APP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApplicationUrlServiceUnitTest {

    private static final TenantKey TENANT = TenantKey.valueOf("TEST");

    private XmRequestContext context;
    private TenantDomainsService tenantDomainsService;
    private TenantProperties tenantProperties;
    private ApplicationUrlService service;

    @Before
    public void setUp() {
        XmRequestContextHolder holder = mock(XmRequestContextHolder.class);
        context = mock(XmRequestContext.class);
        when(holder.getContext()).thenReturn(context);

        TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TENANT));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        tenantDomainsService = mock(TenantDomainsService.class);
        when(tenantDomainsService.getDomains(TENANT)).thenReturn(Set.of("app.tenant.com"));

        tenantProperties = new TenantProperties();
        TenantPropertiesService tenantPropertiesService = mock(TenantPropertiesService.class);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

        service = new ApplicationUrlService(holder, tenantContextHolder, tenantDomainsService, tenantPropertiesService);
    }

    @Test
    public void shouldReturnWebAppUrlWhenHostIsAllowed() {
        mockContext("https", "api.tenant.com", "443", "https://APP.tenant.com");

        assertEquals("https://APP.tenant.com", service.getApplicationUrl());
    }

    @Test(expected = BusinessException.class)
    public void shouldThrowWhenWebAppUrlHostIsNotAllowed() {
        mockContext("https", "app.tenant.com", "443", "https://evil.com");

        service.getApplicationUrl();
    }

    @Test
    public void shouldBuildUrlFromDomainWhenWebAppUrlIsAbsent() {
        mockContext("https", "app.tenant.com", "8443", null);

        assertEquals("https://app.tenant.com:8443", service.getApplicationUrl());
    }

    @Test(expected = BusinessException.class)
    public void shouldThrowWhenDomainIsNotAllowed() {
        mockContext("https", "evil.com", "80", null);

        service.getApplicationUrl();
    }

    @Test(expected = BusinessException.class)
    public void shouldThrowWhenSchemeIsNotHttp() {
        mockContext("javascript", "app.tenant.com", "80", null);

        service.getApplicationUrl();
    }

    @Test
    public void shouldReturnAllowedDomain() {
        mockContext("https", "App.Tenant.com", "443", null);

        assertEquals("App.Tenant.com", service.getDomain());
    }

    @Test(expected = BusinessException.class)
    public void shouldThrowWhenRequestDomainIsNotAllowed() {
        mockContext("https", "evil.com", "443", null);

        service.getDomain();
    }

    @Test
    public void shouldAllowHostFromUaaProperties() {
        when(tenantDomainsService.getDomains(TENANT)).thenReturn(Collections.emptySet());
        tenantProperties.getSecurityPolicy().setHosts(List.of("App.Other.com"));

        mockContext("https", "app.other.com", "80", null);
        assertEquals("https://app.other.com", service.getApplicationUrl());

        mockContext("https", "app.other.com", "443", "https://evil.com");
        assertThrows(BusinessException.class, () -> service.getApplicationUrl());
    }

    @Test
    public void shouldSkipCheckWhenTenantHostsAreNotConfigured() {
        when(tenantDomainsService.getDomains(TENANT)).thenReturn(Collections.emptySet());
        mockContext("http", "localhost", "8080", "https://evil.com");

        assertEquals("https://evil.com", service.getApplicationUrl());
        assertEquals("localhost", service.getDomain());
    }

    @Test
    public void shouldKeepUrlWithoutPortWhenTenantHostsAreNotConfigured() {
        when(tenantDomainsService.getDomains(TENANT)).thenReturn(Collections.emptySet());
        mockContext("http", "localhost", "80", null);

        assertEquals("http://localhost", service.getApplicationUrl());
    }

    private void mockContext(String scheme, String domain, String port, String webApp) {
        when(context.getValue(REQUEST_CTX_PROTOCOL, String.class)).thenReturn(scheme);
        when(context.getValue(REQUEST_CTX_DOMAIN, String.class)).thenReturn(domain);
        when(context.getValue(REQUEST_CTX_PORT, String.class)).thenReturn(port);
        when(context.getValue(REQUEST_CTX_WEB_APP, String.class)).thenReturn(webApp);
    }
}
