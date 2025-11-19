package com.icthh.xm.uaa.service.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.service.kafka.UaaKafkaTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
    Use xm-commons WebSocketIntegrationService after migration to java 21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketIntegrationService {

    public static final String SOCKET_KEY_LOGOUT = "system-user-logout";

    private static final String SOCKET_KAFKA_TRANSPORT_SUFFIX = "_ws_out_";
    private static final String TOPIC_SUFFIX = "system_logout";

    private final UaaKafkaTemplateService kafkaTemplateService;
    private final TenantContextHolder tenantContextHolder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Send message to default kafka logout websocket topic.
     * Header should contain logout userKey
     * @param socketKey     system-user-logout
     * @param dataHolder    data map containing userKey
     */
    public void sendSystemUserLogoutKafkaSocket(String socketKey, Map<String, Object> dataHolder) {
        log.info("Try to send kafka socket event: socketKey={}", socketKey);
        try {
            String tenantKey = tenantContextHolder.getTenantKey();
            String topic = tenantKey + SOCKET_KAFKA_TRANSPORT_SUFFIX + TOPIC_SUFFIX;
            String messageJson = toKafkaSocketMessageJson(socketKey, socketKey);

            kafkaTemplateService.send(topic, messageJson, dataHolder);

        } catch (Exception e) {
            log.error("Failed to send kafka socket message to: {}, message: {}", socketKey, dataHolder, e);
        }
    }

    private String toKafkaSocketMessageJson(String socketKey, String messageJson) {
        try {
            Map<String, String> socketMessage = Map.of(
                "socketKey", socketKey,
                "message", messageJson
            );
            return objectMapper.writeValueAsString(socketMessage);

        } catch (JsonProcessingException e) {
            log.error("Failed to create kafka socket message to: {}, message: {}", socketKey, messageJson, e);
            throw new RuntimeException(e);
        }
    }
}
