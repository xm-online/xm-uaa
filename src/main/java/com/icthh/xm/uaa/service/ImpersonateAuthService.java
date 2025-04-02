package com.icthh.xm.uaa.service;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.AuthRole;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.InboundTenantRole;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.RoleConfiguration;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.ImpersonateAuthenticationToken;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;


@Slf4j
@Service
public class ImpersonateAuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthorizationServerTokenServices tokenServices;
    private final ClientDetailsService clientDetailsService;
    private final XmAuthenticationContextHolder authenticationContextHolder;
    private final AuthenticationService authenticationService;
    private final TenantPropertiesService tenantPropertiesService;
    private final TenantContextHolder tenantContextHolder;

    public ImpersonateAuthService(
        @Qualifier("authenticationManagerBean")
        AuthenticationManager authenticationManager,
        AuthorizationServerTokenServices tokenServices,
        ClientDetailsService clientDetailsService,
        XmAuthenticationContextHolder authenticationContextHolder,
        AuthenticationService authenticationService,
        TenantPropertiesService tenantPropertiesService,
        TenantContextHolder tenantContextHolder
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenServices = tokenServices;
        this.clientDetailsService = clientDetailsService;
        this.authenticationContextHolder = authenticationContextHolder;
        this.authenticationService = authenticationService;
        this.tenantPropertiesService = tenantPropertiesService;
        this.tenantContextHolder = tenantContextHolder;
    }

    public OAuth2AccessToken impersonateLogin(@PathVariable("login") String login, String inboundTenant) {

        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        String roleKey = getRoleKey(authContext.getAuthorities());
        log.info(logPrefix(inboundTenant) + " try impersonate login to '{}'", login);

        Authentication authentication = authenticationManager.authenticate(
            new ImpersonateAuthenticationToken(login)
        );

        String targetRole = checkImpersonateLoginIsAllowed(inboundTenant, roleKey, authentication);
        OAuth2AccessToken token = createToken(authentication, targetRole);

        log.info(logPrefix(inboundTenant) + " successfully impersonate login to '{}' by role '{}'", login, targetRole);
        return token;
    }

    private String logPrefix(String inboundTenant) {
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        String roleKey = getRoleKey(authContext.getAuthorities());
        return "User " + authContext.getRequiredLogin() + " from tenant " + inboundTenant + " with role " + roleKey + " ";
    }

    private String checkImpersonateLoginIsAllowed(String inboundTenant, String inboundRole, Authentication authentication) {
        AuthRole roleConfig = impersonateConfiguration(inboundTenant, inboundRole);
        String userRoleKey = getRoleKey(authentication.getAuthorities());
        var authToRole = findTargetRole(inboundTenant, roleConfig, userRoleKey).getAuthenticateAsRole();
        String roleKey = isNoneBlank(authToRole) ? authToRole : userRoleKey;
        checkIsAllowedByLogin(inboundTenant, roleConfig, authentication);
        return roleKey;
    }

    private void checkIsAllowedByLogin(String inboundTenant, AuthRole roleConfig, Authentication authentication) {
        DomainUserDetails userDetails = (DomainUserDetails) authentication.getPrincipal();
        if (TRUE.equals(roleConfig.getCanAuthToAllLogins())) {
            return;
        }

        Set<String> roles = new HashSet<>(roleConfig.getCanAuthToLogins());
        if (userDetails.getLogins().stream().noneMatch(it -> roles.contains(it.getLogin()))) {
            log.error(logPrefix(inboundTenant) + " is not allowed to impersonate login to logins '{}'." +
                    " Pls specify login in 'canAuthToLogins' or set 'canAuthByAllLogins' as true in 'rolesMapping' configuration",
                userDetails.getLogins());
            throw new BusinessException("error.impersonate.auth.login.not.allowed",
                "Impersonate login not allowed. Check log to details.");
        }
    }


    private OAuth2AccessToken createToken(Authentication authentication, String targetRole) {
        ClientDetails client = getClient();
        var authToken = new UsernamePasswordAuthenticationToken(
            authentication.getPrincipal(),
            authentication.getCredentials(),
            List.of(new SimpleGrantedAuthority(targetRole))
        );
        OAuth2Request oAuth2Request = buildTokenRequest(client).createOAuth2Request(client);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authToken);
        return tokenServices.createAccessToken(oAuth2Authentication);
    }

    private TokenRequest buildTokenRequest(ClientDetails client) {
        HashSet<String> scopes = new HashSet<>(List.of("openid"));
        return new TokenRequest(new HashMap<>(), client.getClientId(), scopes, null);
    }

    private ClientDetails getClient() {
        String clientId = authenticationContextHolder.getContext().getClientId()
            .orElse(authenticationService.findClientId());
        return clientDetailsService.loadClientByClientId(clientId);
    }

    private static String getRoleKey(Collection<? extends GrantedAuthority> authorities) {
        if (authorities.size() != 1) {
            String message = "Multi role strategy is not supported for impersonate auth";
            log.error(message);
            throw new BusinessException("error.multi.role.strategy.not.supported", message);
        }
        return authorities.iterator().next().getAuthority();
    }

    private AuthRole impersonateConfiguration(String inboundTenant, String inboundRole) {
        ImpersonateAuth impersonateAuth = tenantPropertiesService.getTenantProps().getImpersonateAuth();
        if (impersonateAuth == null || !TRUE.equals(impersonateAuth.getEnabled())) {
            log.error("Impersonate auth is not enabled. Check impersonateAuth configuration in uaa.yml");
            throw new BusinessException("error.impersonate.auth.not.enabled", "Impersonate auth is not enabled");
        }

        List<AuthRole> roleMapping = firstNonNull(impersonateAuth.getRolesMapping(), List.of());
        List<AuthRole> roleConfigs = roleMapping.stream().filter(roleConfig ->
                Optional.ofNullable(roleConfig.getCanAuthFromInboundTenantRole())
                    .filter(it -> checkTenant(inboundTenant, it))
                    .filter(it -> inboundRole.equalsIgnoreCase(it.getRole()))
                    .isPresent()
            ).collect(toList());

        if (roleConfigs.size() != 1) {
            log.error("'rolesMapping' should have exactly one configuration for tenant '{}' and role '{}'", inboundTenant, inboundRole);
            throw new BusinessException("error.impersonate.auth.login.not.allowed",
                "Impersonate login not allowed. Check log to details");
        }

        return roleConfigs.get(0);
    }

    private RoleConfiguration findTargetRole(String inboundTenant, AuthRole roleConfig, String userRoleKey) {
        List<RoleConfiguration> canAuthToRoles = firstNonNull(roleConfig.getCanAuthToRoles(), List.of());
        return canAuthToRoles.stream()
            .filter(it -> userRoleKey.equalsIgnoreCase(it.getRole()))
            .findFirst()
            .orElseThrow(() -> {
                log.error(logPrefix(inboundTenant) + "' is not allowed to impersonate login to role '" + userRoleKey + "'."
                    + " To allow login to this role add it to 'canAuthToRoles' in 'rolesMapping' configuration");
                return new BusinessException("error.impersonate.auth.login.not.allowed",
                    "Impersonate login not allowed. Check log to details");
            });
    }

    private boolean checkTenant(String inboundTenant, InboundTenantRole it) {
        if (inboundTenant.equalsIgnoreCase(tenantContextHolder.getTenantKey()) && isBlank(it.getTenant())) {
            return true;
        }
        return inboundTenant.equalsIgnoreCase(it.getTenant());
    }

}
