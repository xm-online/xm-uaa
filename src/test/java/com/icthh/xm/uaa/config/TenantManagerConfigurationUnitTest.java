package com.icthh.xm.uaa.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerConfigurationUnitTest {

    private TenantManager tenantManager;

    private TenantConfigProvisioner configProvisioner;

    @Spy
    private TenantManagerConfiguration configuration = new TenantManagerConfiguration();

    @Mock
    private TenantDatabaseProvisioner databaseProvisioner;
    @Mock
    private TenantListProvisioner tenantListProvisioner;
    @Mock
    private TenantConfigRepository tenantConfigRepository;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private PermissionProperties permissionProperties;
    @Mock
    private ResourceLoader resourceLoader;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(configuration.getApplicationName()).thenReturn("uaa");

        when(permissionProperties.getRolesSpecPath()).thenReturn("/config/tenants/{tenantName}/roles.yml");
        when(permissionProperties.getPermissionsSpecPath()).thenReturn("/config/tenants/{tenantName}/permissions.yml");
        when(applicationProperties.getTenantPropertiesPathPattern()).thenReturn("/config/tenants/{tenantName}/uaa/uaa.yml");
        when(applicationProperties.getTenantLoginPropertiesPathPattern()).thenReturn("/config/tenants/{tenantName}/uaa/logins.yml");

        configProvisioner = spy(configuration.tenantConfigProvisioner(tenantConfigRepository,
                                                                      permissionProperties,
                                                                      applicationProperties,
                                                                      resourceLoader));

        tenantManager = configuration.tenantManager(databaseProvisioner, configProvisioner, tenantListProvisioner);
    }

    @Test
    public void testCreateTenantConfigProvisioning() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        List<Configuration> configurations = new ArrayList<>();
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/roles.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/permissions.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/uaa/uaa.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/uaa/logins.yml").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/uaa/emails/en/activationEmail.ftl").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/uaa/emails/en/creationEmail.ftl").build());
        configurations.add(Configuration.of().path("/config/tenants/{tenantName}/uaa/emails/en/passwordResetEmail.ftl").build());


        verify(tenantConfigRepository).createConfigsFullPath(eq("newtenant"), eq(configurations));

    }

    @Test
    public void testCreateTenantProvisioningOrder() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        InOrder inOrder = Mockito.inOrder(tenantListProvisioner, databaseProvisioner, configProvisioner);

        inOrder.verify(tenantListProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(databaseProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(configProvisioner).createTenant(any(Tenant.class));

        verifyNoMoreInteractions(tenantListProvisioner, databaseProvisioner, configProvisioner);
    }

}
