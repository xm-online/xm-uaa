package com.icthh.xm.uaa.repository.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.domain.kafka.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProfileEventProducer {

    private final KafkaTemplate<String, String> template;
    private final JavaTimeModule module = new JavaTimeModule();
    private final ObjectMapper mapper = new ObjectMapper().configure(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(module);

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${application.kafka-system-queue}")
    private String topicName;

    /**
     * Build message for kafka's event.
     *
     * @param data      data for kafka's event content
     * @param eventType event type for kafka's event content
     */
    public String createEventJson(Object data, String eventType) {
        try {
            SystemEvent event = new SystemEvent();
            event.setEventId(MdcUtils.getRid());
            event.setMessageSource(appName);
            event.setEventType(eventType);
            event.setTenantKey(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder));
            event.setUserLogin(authContextHolder.getContext().getRequiredLogin());
            event.setStartDate(Instant.now().toString());
            event.setData(data);
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Error creating system queue event, error: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Send event to kafka.
     *
     * @param content the event content
     */
    @Async
    public void send(String content) {
        if (!StringUtils.isBlank(content)) {
            log.debug("Sending kafka event to topic = '{}', data = '{}'", topicName, content);
            template.send(topicName, content);
        }
    }

}

