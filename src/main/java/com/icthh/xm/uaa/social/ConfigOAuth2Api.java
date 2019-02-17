package com.icthh.xm.uaa.social;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.springframework.social.oauth2.TokenStrategy.AUTHORIZATION_HEADER;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.domain.properties.TenantProperties.UserInfoMapping;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.ApiBinding;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.oauth2.TokenStrategy;

@Slf4j
public class ConfigOAuth2Api extends AbstractOAuth2ApiBinding implements ApiBinding {
    private final String accessToken;
    private final Social social;
    private final SocialUserInfoMapper socialUserInfoMapper;

    public ConfigOAuth2Api(String accessToken, Social social, SocialUserInfoMapper socialUserInfoMapper) {
        super(accessToken, getTokenStrategy(social));
        this.accessToken = accessToken;
        this.social = social;
        this.socialUserInfoMapper = socialUserInfoMapper;
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
        return fetchUserInfo(socialUserInfoMapper::toConnectionValues);
    }

    private <T> T fetchUserInfo(BiFunction<UserInfoMapping, Map<String,  Object>, T> mapper) {
        Map<String, Object> userInfo = getRestTemplate().getForObject(social.getUserInfoUri(), Map.class);
        log.info("User info {}", userInfo);
        UserInfoMapping socialMapping = social.getUserInfoMapping();
        socialMapping = socialMapping != null ? socialMapping : new UserInfoMapping();
        return mapper.apply(socialMapping, userInfo);
    }

    public UserProfile fetchUserProfile() {
        return fetchUserInfo(socialUserInfoMapper::toUserProfile);
    }

    public SocialUserInfo fetchSocialUser() {
        return fetchUserInfo(socialUserInfoMapper::toSocialUserInfo);
    }
}
