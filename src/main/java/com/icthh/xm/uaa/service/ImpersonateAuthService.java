package com.icthh.xm.uaa.service;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.ImpersonateLoginAuditEvent;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.AuthRole;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.InboundTenantRole;
import com.icthh.xm.uaa.domain.properties.TenantProperties.ImpersonateAuth.RoleConfiguration;
import com.icthh.xm.uaa.repository.ImpersonateLoginAuditEventRepository;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.ImpersonateAuthenticationToken;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    public static final String IMPERSONATE_INBOUND_ROLE = "impersonateInboundRole";
    public static final String IMPERSONATE_INBOUND_ROLE_TENANT = "impersonateInboundRoleTenant";
    public static final String IMPERSONATE_INBOUND_LOGIN = "impersonateInboundLogin";
    public static final String OPENID = "openid";

    private final AuthenticationManager authenticationManager;
    private final AuthorizationServerTokenServices tokenServices;
    private final ClientDetailsService clientDetailsService;
    private final XmAuthenticationContextHolder authenticationContextHolder;
    private final AuthenticationService authenticationService;
    private final TenantPropertiesService tenantPropertiesService;
    private final TenantContextHolder tenantContextHolder;
    private final ImpersonateLoginAuditEventRepository auditEventRepository;

    public ImpersonateAuthService(
        @Qualifier("authenticationManagerBean")
        AuthenticationManager authenticationManager,
        AuthorizationServerTokenServices tokenServices,
        ClientDetailsService clientDetailsService,
        XmAuthenticationContextHolder authenticationContextHolder,
        AuthenticationService authenticationService,
        TenantPropertiesService tenantPropertiesService,
        TenantContextHolder tenantContextHolder,
        ImpersonateLoginAuditEventRepository auditEventRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenServices = tokenServices;
        this.clientDetailsService = clientDetailsService;
        this.authenticationContextHolder = authenticationContextHolder;
        this.authenticationService = authenticationService;
        this.tenantPropertiesService = tenantPropertiesService;
        this.tenantContextHolder = tenantContextHolder;
        this.auditEventRepository = auditEventRepository;
    }

    public OAuth2AccessToken impersonateLogin(@PathVariable("login") String login, String inboundTenant) {

        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        String roleKey = getRoleKey(authContext.getAuthorities());
        log.info(logPrefix(inboundTenant) + " try impersonate login to '{}'", login);

        Authentication authentication = authenticationManager.authenticate(
            new ImpersonateAuthenticationToken(login)
        );

        DomainUserDetails processedAuthentication = processImpersonateLoginByConfig(inboundTenant, roleKey, authentication);
        OAuth2AccessToken token = createToken(authentication, processedAuthentication);

        writeAuditEvent(inboundTenant, roleKey, processedAuthentication);

        log.info(logPrefix(inboundTenant) + " successfully impersonate login to '{}' by role '{}'", login, processedAuthentication.getAuthorities());
        return token;
    }

    private void writeAuditEvent(String inboundTenant, String inboundRoleKey,
                                 DomainUserDetails processedAuthentication) {
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        ImpersonateLoginAuditEvent auditEvent = new ImpersonateLoginAuditEvent();
        auditEvent.setEventDate(Instant.now());
        auditEvent.setInboundLogin(authContext.getRequiredLogin());
        auditEvent.setInboundTenant(inboundTenant);
        auditEvent.setInboundRole(inboundRoleKey);
        auditEvent.setInboundUserKey(authContext.getRequiredUserKey());
        auditEvent.setUserKey(processedAuthentication.getUserKey());
        auditEvent.setUserRole(processedAuthentication.getAuthorities().iterator().next().getAuthority());
        auditEventRepository.save(auditEvent);
    }

    private String logPrefix(String inboundTenant) {
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        String roleKey = getRoleKey(authContext.getAuthorities());
        return "User '" + authContext.getRequiredLogin() + "' from tenant '" + inboundTenant + "' with role '" + roleKey + "' ";
    }

    private DomainUserDetails processImpersonateLoginByConfig(String inboundTenant, String inboundRole, Authentication authentication) {
        String userRoleKey = getRoleKey(authentication.getAuthorities());

        AuthRole roleConfig = impersonateConfiguration(inboundTenant, inboundRole);
        var authToRole = findTargetRole(inboundTenant, roleConfig, userRoleKey).getAuthenticateAsRole();
        checkIsAllowedByLogin(inboundTenant, roleConfig, authentication);

        Integer tokenValidityTime = getTokenValidityTime(authentication, roleConfig);

        DomainUserDetails userDetails = (DomainUserDetails) authentication.getPrincipal();
        String roleKey = isNoneBlank(authToRole) ? authToRole : userRoleKey;
        DomainUserDetails updatedUserDetails = copyDomainUserDetails(userDetails, roleKey, tokenValidityTime);
        updatedUserDetails.getAdditionalDetails().put(IMPERSONATE_INBOUND_ROLE, inboundRole);
        updatedUserDetails.getAdditionalDetails().put(IMPERSONATE_INBOUND_ROLE_TENANT, inboundTenant);
        XmAuthenticationContext authContext = authenticationContextHolder.getContext();
        updatedUserDetails.getAdditionalDetails().put(IMPERSONATE_INBOUND_LOGIN, authContext.getRequiredLogin());
        return updatedUserDetails;
    }

    private DomainUserDetails copyDomainUserDetails(DomainUserDetails domainUserDetails,
                                                    String roleKey, Integer tokenValidityTime) {
        var copy = new DomainUserDetails(
            domainUserDetails.getUsername(),
            UUID.randomUUID().toString(),
            List.of(() -> roleKey),
            domainUserDetails.getTenant(),
            domainUserDetails.getUserKey(),
            domainUserDetails.getAuthOtpCode(),
            domainUserDetails.getAuthOtpCodeCreationDate(),
            domainUserDetails.isTfaEnabled(),
            domainUserDetails.getTfaOtpSecret(),
            domainUserDetails.getTfaOtpChannelType().orElse(null),
            tokenValidityTime,
            domainUserDetails.getRefreshTokenValiditySeconds(),
            domainUserDetails.getTfaAccessTokenValiditySeconds(),
            domainUserDetails.isAutoLogoutEnabled(),
            domainUserDetails.getAutoLogoutTimeoutSeconds(),
            domainUserDetails.getLogins(),
            domainUserDetails.getLangKey()
        );
        copy.getAdditionalDetails().putAll(domainUserDetails.getAdditionalDetails());
        return copy;
    }

    private Integer getTokenValidityTime(Authentication authentication, AuthRole roleConfig) {
        DomainUserDetails userDetails = (DomainUserDetails) authentication.getPrincipal();
        return ofNullable(roleConfig.getTokenValiditySeconds())
            .or(() -> ofNullable(
                tenantPropertiesService.getTenantProps().getImpersonateAuth().getTokenValiditySeconds()
            )).orElse(userDetails.getAccessTokenValiditySeconds());
    }

    private void checkIsAllowedByLogin(String inboundTenant, AuthRole roleConfig, Authentication authentication) {
        DomainUserDetails userDetails = (DomainUserDetails) authentication.getPrincipal();
        if (TRUE.equals(roleConfig.getCanAuthByAllLogins())) {
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


    private OAuth2AccessToken createToken(Authentication authentication, DomainUserDetails domainUserDetails) {
        ClientDetails client = getClient();
        var authToken = new UsernamePasswordAuthenticationToken(
            domainUserDetails,
            authentication.getCredentials(),
            domainUserDetails.getAuthorities()
        );
        OAuth2Request oAuth2Request = buildTokenRequest(client).createOAuth2Request(client);
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authToken);
        return tokenServices.createAccessToken(oAuth2Authentication);
    }

    private TokenRequest buildTokenRequest(ClientDetails client) {
        HashSet<String> scopes = new HashSet<>(List.of(OPENID));
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
                ofNullable(roleConfig.getCanAuthFromInboundTenantRole())
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
                log.error(logPrefix(inboundTenant) + " is not allowed to impersonate login to role '" + userRoleKey + "'."
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
