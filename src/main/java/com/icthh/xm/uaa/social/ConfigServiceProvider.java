package com.icthh.xm.uaa.social;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;

public class ConfigServiceProvider implements OAuth2ServiceProvider<ConfigOAuth2Api> {

    private final Social social;
    private final SocialUserInfoMapper socialUserInfoMapper;
    private final ConfigOAuth2Template oAuth2Template;

    public ConfigServiceProvider(Social social, SocialUserInfoMapper socialUserInfoMapper) {
        this.social = social;
        this.socialUserInfoMapper = socialUserInfoMapper;
        this.oAuth2Template = createOAuth2Template(social);
    }

    protected ConfigOAuth2Template createOAuth2Template(Social social) {
        ConfigOAuth2Template oAuth2Template = new ConfigOAuth2Template(social.getClientId(), social.getClientSecret(),
                                                           social.getAuthorizeUrl(), social.getAccessTokenUrl());
        oAuth2Template.setUseParametersForClientAuthentication(true);
        return oAuth2Template;
    }

    @Override
    public OAuth2Operations getOAuthOperations() {
        return oAuth2Template;
    }

    @Override
    public ConfigOAuth2Api getApi(String accessToken) {
        return new ConfigOAuth2Api(accessToken, social, socialUserInfoMapper);
    }
}
