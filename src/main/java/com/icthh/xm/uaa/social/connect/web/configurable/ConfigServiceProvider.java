package com.icthh.xm.uaa.social.connect.web.configurable;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;

public class ConfigServiceProvider extends AbstractOAuth2ServiceProvider<ConfigOAuth2Api> {

    public ConfigServiceProvider(TenantProperties.Social social) {
        super(new ConfigOAuth2Template(social));
    }

    public ConfigOAuth2Api getApi(String accessToken) {
        return new ConfigOAuth2Api(accessToken);
    }
}
