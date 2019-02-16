package com.icthh.xm.uaa.social;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.social.oauth2.TokenStrategy.AUTHORIZATION_HEADER;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.domain.properties.TenantProperties.UserInfoMapping;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.ApiBinding;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.oauth2.TokenStrategy;

@Slf4j
public class ConfigOAuth2Api extends AbstractOAuth2ApiBinding implements ApiBinding {
    private final String accessToken;
    private final Social social;

    public ConfigOAuth2Api(String accessToken, Social social) {
        super(accessToken, getTokenStrategy(social));
        this.accessToken = accessToken;
        this.social = social;
    }

    private static TokenStrategy getTokenStrategy(Social social) {
        String tokenStrategy = social.getTokenStrategy();
        return tokenStrategy != null ? TokenStrategy.valueOf(tokenStrategy) : AUTHORIZATION_HEADER;
    }

    @Override
    public boolean isAuthorized() {
        return accessToken != null;
    }

    public ConnectionValuesDto fetchConnectionValues() {
        return fetchUserInfo((userInfo, mapping) ->
                                 new ConnectionValuesDto(
                                     map(mapping::getId, userInfo),
                                     map(mapping::getName, userInfo),
                                     map(mapping::getProfileUrl, userInfo),
                                     map(mapping::getImageUrl, userInfo)
                                 )
                            );
    }

    private <T> T fetchUserInfo(BiFunction<Map<String, Object>, UserInfoMapping, T> mapper) {
        Map<String, Object> userInfo = getRestTemplate().getForObject(social.getUserInfoUri(), Map.class);
        log.info("User info {}", userInfo);
        UserInfoMapping socialMapping = social.getUserInfoMapping();
        socialMapping = socialMapping != null ? socialMapping : new UserInfoMapping();
        return mapper.apply(userInfo, socialMapping);
    }

    @SuppressWarnings("unchecked")
    public UserProfile fetchUserProfile() {
        return fetchUserInfo((userInfo, mapping) ->
                                 new UserProfile(
                                     map(mapping::getId, userInfo),
                                     map(mapping::getName, userInfo),
                                     map(mapping::getFirstName, userInfo),
                                     map(mapping::getLastName, userInfo),
                                     map(() -> defaultIfBlank(mapping.getEmail(), "email"), userInfo),
                                     map(mapping::getUsername, userInfo)
                                 )
                            );
    }

    public SocialUserDto fetchSocialUser() {
        return fetchUserInfo((userInfo, mapping) ->
                                 new SocialUserDto(
                                     map(mapping::getId, userInfo),
                                     map(mapping::getName, userInfo),
                                     map(mapping::getFirstName, userInfo),
                                     map(mapping::getLastName, userInfo),
                                     map(() -> defaultIfBlank(mapping.getEmail(), "email"), userInfo),
                                     map(mapping::getUsername, userInfo),
                                     map(mapping::getProfileUrl, userInfo),
                                     map(mapping::getImageUrl, userInfo),
                                     map(mapping::getPhoneNumber, userInfo),
                                     map(mapping::getLangKey, userInfo)
                                 )
                            );
    }

    private String map(Supplier<String> mapping, Map<String, Object> userInfo) {
        if (isBlank(mapping.get())) {
            return null;
        }
        Object value = userInfo;
        for(String property: mapping.get().split(".")) {
            value = getProperty(Optional.ofNullable(value), property);
        }
        return value == null ? null : String.valueOf(value);
    }

    private Object getProperty(Optional<Object> object, String property) {
        return object.filter(Map.class::isInstance)
                     .map (Map.class::cast)
                     .map(map -> map.get(property))
                     .orElse(null);
    }
}
