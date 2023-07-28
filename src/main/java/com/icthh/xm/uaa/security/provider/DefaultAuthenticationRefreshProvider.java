package com.icthh.xm.uaa.security.provider;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.uaa.lep.keyresolver.OptionalProfileHeaderResolver;
import com.icthh.xm.uaa.security.AuthenticationRefreshProvider;
import com.icthh.xm.uaa.security.DomainUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.uaa.config.Constants.AUTH_ADDITIONAL_DETAILS;
import static com.icthh.xm.uaa.config.Constants.AUTH_LOGINS_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;

@Component
@LepService(group = "security.provider")
@IgnoreLogginAspect
public class DefaultAuthenticationRefreshProvider implements AuthenticationRefreshProvider {

    @SuppressWarnings("unchecked")
    @Override
    @LogicExtensionPoint(value = "Refresh", resolver = OptionalProfileHeaderResolver.class)
    public Authentication refresh(OAuth2Authentication authentication) {
        Object detailsValue = authentication.getDetails();
        if (detailsValue instanceof Map) {
            String username = (String) authentication.getPrincipal();
            Map<String, Object> details = (Map) detailsValue;

            DomainUserDetails user = new DomainUserDetails(username,
                                                           "",
                                                           authentication.getAuthorities(),
                                                           getMapValueStr(details, AUTH_TENANT_KEY),
                                                           getMapValueStr(details, AUTH_USER_KEY),
                                                           false,
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           null,
                                                           false,
                                                           null,
                                                           getMapValueList(details, AUTH_LOGINS_KEY)
            );

            Optional.of(details)
                    .map(map -> map.get(AUTH_ADDITIONAL_DETAILS))
                    .map(Map.class::cast)
                    .ifPresent(map -> user.getAdditionalDetails().putAll(map));

            return new UsernamePasswordAuthenticationToken(user, "", authentication.getAuthorities());
        }
        return authentication;
    }

    private static List getMapValueList(Map<String, Object> map, String key) {
        return getMapValue(map, key, List.class);
    }

    private static String getMapValueStr(Map<String, Object> map, String key) {
        return getMapValue(map, key, String.class);
    }

    private static <T> T getMapValue(Map<String, Object> map, String key, Class<T> type) {
        return type.cast(map.get(key));
    }

}
