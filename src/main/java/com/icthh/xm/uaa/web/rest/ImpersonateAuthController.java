package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.service.ImpersonateAuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ImpersonateAuthController {

    private final ImpersonateAuthService impersonateAuthService;
    private final TenantContextHolder tenantContextHolder;

    @Timed
    @PreAuthorize("hasPermission(null, 'UAA.IMPERSONATE_LOGIN')")
    @PrivilegeDescription("Privilege to create a new role")
    @PostMapping("/oauth/impersonate/{login}")
    public ResponseEntity<?> impersonateLogin(@PathVariable("login") String login,
                                              @RequestParam(value = "tenant", required = false) String tenant) {
        String inboundTenant = tenantContextHolder.getTenantKey().toUpperCase();
        if (StringUtils.isBlank(tenant) || tenant.equalsIgnoreCase(inboundTenant)) {
            return ResponseEntity.ok(impersonateAuthService.impersonateLogin(login, inboundTenant));
        }

        PrivilegedTenantContext privilegedContext = tenantContextHolder.getPrivilegedContext();
        var token = privilegedContext.execute(buildTenant(tenant.toUpperCase()), () ->
            impersonateAuthService.impersonateLogin(login, inboundTenant)
        );
        return ResponseEntity.ok(token);
    }
}

