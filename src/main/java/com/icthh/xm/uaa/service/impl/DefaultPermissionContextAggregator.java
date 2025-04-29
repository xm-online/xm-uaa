package com.icthh.xm.uaa.service.impl;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.service.PermissionContextAggregator;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.PermissionContextDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.icthh.xm.commons.config.client.utils.RequestUtils.createAuthHeaders;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Transactional
@LepService(group = "service.permission.load")
public class DefaultPermissionContextAggregator implements PermissionContextAggregator {

    private final String permissionContextPathPattern;
    private final TenantPropertiesService tenantPropertiesService;
    private final RestTemplate restTemplate;

    public DefaultPermissionContextAggregator(ApplicationProperties applicationProperties,
                                              TenantPropertiesService tenantPropertiesService,
                                              @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.permissionContextPathPattern = applicationProperties.getPermissionContextPathPattern();
        this.tenantPropertiesService = tenantPropertiesService;
        this.restTemplate = restTemplate;
    }

    @Override
    @LogicExtensionPoint("LoadServicePermissions")
    public Map<String, PermissionContextDto> loadPermissionsFromServices(String userKey) {
        List<String> services = tenantPropertiesService.getTenantProps().getContextPermission().getServices();

        Map<String, CompletableFuture<?>> contextFutures = services.stream()
            .collect(toMap(Function.identity(), s -> getContextFromService(s, userKey)));

        CompletableFuture.allOf(contextFutures.values().toArray(CompletableFuture[]::new)).join();

        return contextFutures.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> (PermissionContextDto) e.getValue().join()));
    }

    private CompletableFuture<PermissionContextDto> getContextFromService(String service, String userKey) {
        log.info("Get permission context from {} by userKey: {}", service, userKey);
        HttpEntity<Void> request = new HttpEntity<>(createAuthHeaders());

        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = new URI(String.format("http://%s/%s", service, permissionContextPathPattern));
                String paramUri = UriComponentsBuilder.fromUri(uri).queryParam("userKey", userKey).toUriString();
                return restTemplate.exchange(paramUri, HttpMethod.GET, request, PermissionContextDto.class).getBody();

            } catch (Exception ex) {
                log.error("Failed to fetch permission context from {} for userKey {}: {}", service, userKey, ex.getMessage(), ex);
                return new PermissionContextDto();
            }
        });
    }
}
