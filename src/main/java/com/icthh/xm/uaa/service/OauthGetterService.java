package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.OauthCredentials;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthGetterService implements RefreshableConfiguration {

    private final ApplicationProperties appProps;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Getter
    @Setter
    private OauthCredentials oauthCredentials;

    @Override
    public void onRefresh(String updatedKey, String config) {
        try {
            if (appProps.getTenantPath().equals(updatedKey)) {
                this.oauthCredentials = mapper.readValue(config, OauthCredentials.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return appProps.getTenantPath().equals(updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }

}
