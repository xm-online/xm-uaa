package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@LepService(group = "security.idp")
public class IdpIdTokenMappingService {

    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;

    private static final String FIRST_NAME_CONFIG_ATTRIBUTE = "firstNameAttribute";
    private static final String LAST_NAME_CONFIG_ATTRIBUTE = "lastNameAttribute";
    private static final String USER_IDENTITY_CONFIG_TYPE = "userIdentityType";
    private static final String USER_IDENTITY_CONFIG_ATTRIBUTE = "userIdentityAttribute";

    @LogicExtensionPoint(value = "MapIdpIdTokenToIdentity")
    public String mapIdpIdTokenToIdentity(OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        Map<String, String> defaultClaimsMapping = getDefaultClaimsMapping();

        String userIdentityAttribute = defaultClaimsMapping.get(USER_IDENTITY_CONFIG_ATTRIBUTE);

        return (String) additionalInformation.get(userIdentityAttribute);
    }

    @LogicExtensionPoint(value = "ValidateIdpIdToken")
    public void validateIdpIdToken(OAuth2AccessToken idpOAuth2IdToken) {
        log.info("No additional LEP for IDP id token validation is applied");
    }

    @LogicExtensionPoint(value = "MapIdpIdTokenToXmUser")
    public UserDTO mapIdpIdTokenToXmUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        Map<String, Object> additionalInformation = idpOAuth2IdToken.getAdditionalInformation();
        UserDTO userDTO = new UserDTO();

        //base info mapping
        Map<String, String> defaultClaimsMapping = getDefaultClaimsMapping();
        String userFirstNameAttribute = defaultClaimsMapping.get(FIRST_NAME_CONFIG_ATTRIBUTE);

        String userLastNameAttribute = defaultClaimsMapping.get(LAST_NAME_CONFIG_ATTRIBUTE);

        userDTO.setFirstName((String) additionalInformation.get(userFirstNameAttribute));
        userDTO.setLastName((String) additionalInformation.get(userLastNameAttribute));
        //login mapping

        String userIdentityType = UserLoginType
            .fromString(defaultClaimsMapping.get(USER_IDENTITY_CONFIG_TYPE))
            .getValue();

        UserLogin emailUserLogin = new UserLogin();
        emailUserLogin.setLogin(userIdentity);
        emailUserLogin.setTypeKey(userIdentityType);

        userDTO.setLogins(List.of(emailUserLogin));
        return userDTO;
    }

    @SneakyThrows
    @LogicExtensionPoint(value = "MapIdpIdTokenToRole")
    public String mapIdpIdTokenToRole(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        TenantProperties tenantProps = tenantPropertiesService.getTenantProps();
        TenantProperties.Security security = tenantProps.getSecurity();

        if (security == null || StringUtils.isEmpty(security.getDefaultUserRole())) {
            log.debug("Default user role for tenant [{}] not specified in configuration.", getTenantKey());
            throw new AuthenticationServiceException("Authentication failed " +
                "cause of tenant [" + getTenantKey() + "] configuration lack.");
        }
        return security.getDefaultUserRole();
    }

    private Map<String, String> getDefaultClaimsMapping() {
        TenantProperties tenantProps = tenantPropertiesService.getTenantProps();
        TenantProperties.Security security = tenantProps.getSecurity();

        TenantProperties.Security.Idp idp = security.getIdp();

        if (idp == null || idp.getDefaultIdpClaimMapping() == null
            || idp.getDefaultIdpClaimMapping().getFirstNameAttribute() == null
            || idp.getDefaultIdpClaimMapping().getLastNameAttribute() == null
            || idp.getDefaultIdpClaimMapping().getUserIdentityAttribute() == null
            || idp.getDefaultIdpClaimMapping().getUserIdentityType() == null
        ) {
            log.debug("User Identity mapping not fully specified in tenant [{}] configuration.", getTenantKey());
            throw new AuthenticationServiceException("Authentication failed " +
                "cause of tenant [" + getTenantKey() + "] configuration lack.");
        }
        TenantProperties.Security.Idp.DefaultIdpClaimMapping defaultIdpClaimMapping = idp.getDefaultIdpClaimMapping();

        return Map.of(
            FIRST_NAME_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getFirstNameAttribute(),
            LAST_NAME_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getLastNameAttribute(),
            USER_IDENTITY_CONFIG_ATTRIBUTE, defaultIdpClaimMapping.getUserIdentityAttribute(),
            USER_IDENTITY_CONFIG_TYPE, defaultIdpClaimMapping.getUserIdentityType()
        );
    }

    private String getTenantKey() {
        return tenantContextHolder.getTenantKey();
    }
}
