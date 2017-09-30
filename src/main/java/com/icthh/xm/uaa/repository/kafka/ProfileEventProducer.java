package com.icthh.xm.uaa.repository.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.service.dto.UserDTO;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEventProducer {

    private final KafkaTemplate<String, String> template;
    private final JavaTimeModule module = new JavaTimeModule();
    private final ObjectMapper mapper = new ObjectMapper().configure(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(module);

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message for kafka's event.
     * @param user user data for kafka's event content
     * @param eventType event type for kafka's event content
     */
    public String createEventJson(UserDTO user, String eventType) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", MDCUtil.getRid());
            map.put("messageSource", appName);
            map.put("tenantInfo", TenantContext.getCurrent());
            map.put("eventType", eventType);
            map.put("startDate", Instant.now().toString());
            map.put("data", user);
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Error creating profile event", e);
        }
        return null;
    }

    /**
     * Send event to kafka.
     * @param content the event content
     */
    @Async
    public void send(String content) {
        if (!StringUtils.isBlank(content)) {
            log.debug("Sending kafka event with data {} to topic {}", content, topicName);
            template.send(topicName, content);
        }
    }
}
