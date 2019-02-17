package com.icthh.xm.uaa.social;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.web.client.RestTemplate;

public class ConfigServiceProvider extends AbstractOAuth2ServiceProvider<ConfigOAuth2Api> {

    private final Social social;
    private final SocialUserInfoMapper socialUserInfoMapper;

    public ConfigServiceProvider(Social social, SocialUserInfoMapper socialUserInfoMapper) {
        super(createOAuth2Template(social));
        this.social = social;
        this.socialUserInfoMapper = socialUserInfoMapper;
    }

    private static OAuth2Template createOAuth2Template(Social social) {
        OAuth2Template oAuth2Template = new OAuth2Template(social.getClientId(), social.getClientSecret(),
                                                           social.getAuthorizeUrl(), social.getAccessTokenUrl());
        oAuth2Template.setUseParametersForClientAuthentication(true);
        return oAuth2Template;
    }

    @Override
    public ConfigOAuth2Api getApi(String accessToken) {
        return new ConfigOAuth2Api(accessToken, social, socialUserInfoMapper);
    }
}
