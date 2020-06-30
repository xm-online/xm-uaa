package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

/**
 * A service that deletes permissions from the database that have common privileges that were removed.
 * Apples only for tenants that have UAA permission mode enabled.
 */
@Service
@RequiredArgsConstructor
public class PermissionUpdateService {

    private final TenantListRepository tenantListRepository;
    private final DatabaseConfigurationSource databaseConfigurationSource;
    private final TenantContextHolder tenantContextHolder;
    private final PermissionsConfigModeProvider permissionsConfigModeProvider;
    private final TenantRoleService tenantRoleService;
    private final AuthenticationService authenticationService;
    private final LepManager lepManager;
    private final XmAuthenticationContextHolder authContextHolder;

    /**
     * Deletes permissions for not active privileges (i.e. not in {@code existingCommonAppPrivileges}
     *
     * @param msName application name
     * @param existingCommonAppPrivileges currently active common privileges
     */
    @SneakyThrows
    public void deleteRemovedPrivileges(String msName, Set<Privilege> existingCommonAppPrivileges) {
        for (String tenantKey : tenantListRepository.getTenants()) {
            tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenantKey.toUpperCase()),
                () -> {
                    initContext();

                    if (PermissionsConfigMode.DATABASE == permissionsConfigModeProvider.getMode()) {
                        Set<String> activePrivileges = Stream.of(
                            existingCommonAppPrivileges,
                            tenantRoleService.getCustomPrivileges().get(msName))
                            .filter(CollectionUtils::isNotEmpty)
                            .flatMap(Collection::stream)
                            .map(Privilege::getKey)
                            .collect(Collectors.toSet());

                        databaseConfigurationSource.deletePermissionsForRemovedPrivileges(msName,
                            activePrivileges);
                    }
                });
        }
    }

    /**
     * Initializes thread-local contexts to be able to make external calls
     */
    @SneakyThrows
    private void initContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl());

        lepManager.beginThreadContext(threadContext -> {
            threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
        authenticationService.authenticate();
    }
}
