package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private static final String NONE_TENANT = "-NONE-";
    private static final String API = "/api";

    private final TenantConfigRepository configRepository;
    private final PermissionProperties properties;
    private final AuthenticationService authenticationService;
    private final TenantConfigRepository tenantConfigRepository;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @SneakyThrows
    public List<String> getEnvironments() {
        String environmentsFile = tenantConfigRepository.getConfigFullPath(null,
            API + properties.getEnvSpecPath());
        return mapper.readValue(environmentsFile, new TypeReference<List<String>>() {
        });
    }

    @SneakyThrows
    public void updateConfigs(List<String> envVars) {

        if (!authenticate()) {
            return;
        }

        if (!updateEnvironmentsConfig(mapper.writeValueAsString(envVars))) {
            return;
        }

        log.info("Environments config was updated.");
    }

    private boolean authenticate() {
        try {
            authenticationService.authenticate();
            return true;
        } catch (Exception e) {
            log.error("Failed to create authentication", e);
        }
        return false;
    }

    private boolean updateEnvironmentsConfig(String content) {
        String path = properties.getEnvSpecPath();
        try {
            configRepository.updateConfigFullPath(null, API + path, content);
            return true;
        } catch (Exception e) {
            log.error("Error updating configuration {} for tenant {}",
                path, NONE_TENANT, e);
        }
        return false;
    }

}
