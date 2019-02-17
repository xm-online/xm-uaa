package com.icthh.xm.uaa.social;

import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;

public class ConfigOAuth2ConnectionFactory extends OAuth2ConnectionFactory<ConfigOAuth2Api> {

    private final Social social;

    public ConfigOAuth2ConnectionFactory(Social social, SocialUserInfoMapper socialUserInfoMapper) {
        super(social.getProviderId(), new ConfigServiceProvider(social, socialUserInfoMapper), new ConfigAdapter());
        this.social = social;
    }

    @Override
    public String getScope() {
        return social.getScope();
    }

}
