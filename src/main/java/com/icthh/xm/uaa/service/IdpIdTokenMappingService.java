package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@LepService(group = "security.idp")
public class IdpIdTokenMappingService {

    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;

    @LoggingAspectConfig(inputDetails = false)
    @LogicExtensionPoint(value = "MapIdpIdTokenToIdentity")
    public String mapIdpIdTokenToIdentity(OAuth2AccessToken idpOAuth2IdToken) {
        return ClaimsExtractor.with(tenantPropertiesService,
                                    idpOAuth2IdToken.getAdditionalInformation(),
                                    getTenantKey()).getUserIdentity();
    }

    @LoggingAspectConfig(inputDetails = false)
    @LogicExtensionPoint(value = "ValidateIdpIdToken")
    public void validateIdpIdToken(OAuth2AccessToken idpOAuth2IdToken) {
        log.info("No additional LEP for IDP id token validation is applied");
    }

    @LoggingAspectConfig(inputExcludeParams = "idpOAuth2IdToken")
    @LogicExtensionPoint(value = "MapIdpIdTokenToXmUser")
    public UserDTO mapIdpIdTokenToXmUser(String userIdentity, OAuth2AccessToken idpOAuth2IdToken) {
        ClaimsExtractor extractor = ClaimsExtractor.with(tenantPropertiesService,
                                                         idpOAuth2IdToken.getAdditionalInformation(),
                                                         getTenantKey());
        UserDTO userDTO = new UserDTO();

        //base info mapping
        userDTO.setFirstName(extractor.getFirstName());
        userDTO.setLastName(extractor.getLastName());

        //login mapping
        UserLogin emailUserLogin = new UserLogin();
        emailUserLogin.setLogin(userIdentity);
        emailUserLogin.setTypeKey(extractor.getUserIdentityType().getValue());

        userDTO.setLogins(List.of(emailUserLogin));
        return userDTO;
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "idpOAuth2IdToken")
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

    private String getTenantKey() {
        return tenantContextHolder.getTenantKey();
    }


    @RequiredArgsConstructor
    private static class ClaimsExtractor {

        private final Map<String, Object> data;
        private final TenantProperties.Security.Idp.DefaultIdpClaimMapping mapping;

        public String getFirstName(){
            return getRequiredValue(mapping.getFirstNameAttribute(), "firstName");
        }
        public String getLastName(){
            return getRequiredValue(mapping.getLastNameAttribute(), "lastName");
        }
        public String getUserIdentity(){
            return getRequiredValue(mapping.getUserIdentityAttribute(), "userIdentity");
        }
        public UserLoginType getUserIdentityType(){
            return UserLoginType.fromString(mapping.getUserIdentityType());
        }

        @SuppressWarnings("unchecked")
        private <T> T getRequiredValue(String key, String attributeName) {
            return (T) Objects.requireNonNull(data.get(key),
                                              "can not extract attribute [" + attributeName + "] from token claim: " + key);
        }

        public static ClaimsExtractor with(TenantPropertiesService tenantPropertiesService, Map<String, Object> data,
                                           String tenant) {
            return Optional.of(tenantPropertiesService.getTenantProps())
                           .map(TenantProperties::getSecurity)
                           .map(TenantProperties.Security::getIdp)
                           .filter(ClaimsExtractor::isClaimMappingValid)
                           .map(TenantProperties.Security.Idp::getDefaultIdpClaimMapping)
                           .map(mapping -> new ClaimsExtractor(data, mapping))
                           .orElseThrow(() -> buildException(tenant));
        }

       static boolean isClaimMappingValid(TenantProperties.Security.Idp idp) {
            return idp != null && idp.getDefaultIdpClaimMapping() != null
                   && idp.getDefaultIdpClaimMapping().getFirstNameAttribute() != null
                   && idp.getDefaultIdpClaimMapping().getLastNameAttribute() != null
                   && idp.getDefaultIdpClaimMapping().getUserIdentityAttribute() != null
                   && idp.getDefaultIdpClaimMapping().getUserIdentityType() != null;
        }

        private static AuthenticationServiceException buildException(String tenant) {
            return new AuthenticationServiceException(
                "Authentication failed for [" + tenant + "] because DefaultIdpClaimMapping is not defined.");
        }

    }

}
