package com.icthh.xm.uaa.service.permission;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.service.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

/**
 * //todo V: doc
 */
@Component
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
