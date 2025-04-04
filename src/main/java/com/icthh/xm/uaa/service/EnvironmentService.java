package com.icthh.xm.uaa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private static final String NONE_TENANT = "-NONE-";
    private static final String API = "/api";

    private final CommonConfigRepository commonConfigRepository;
    private final PermissionProperties properties;
    private final AuthenticationService authenticationService;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @SneakyThrows
    public List<String> getEnvironments() {
        String environmentsFile = commonConfigRepository
            .getConfig(null, Collections.singletonList(properties.getEnvSpecPath()))
            .values().stream()
            .map(Configuration::getContent)
            .findFirst()
            .orElse("---");
        return mapper.readValue(environmentsFile, new TypeReference<List<String>>() {
        });
    }

    @SneakyThrows
    @Async
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
            commonConfigRepository.updateConfigFullPath(Configuration.of()
                                                                     .path(path)
                                                                     .content(content)
                                                                     .build(), null);
            return true;
        } catch (Exception e) {
            log.error("Error updating configuration {} for tenant {}",
                path, NONE_TENANT, e);
        }
        return false;
    }

}
