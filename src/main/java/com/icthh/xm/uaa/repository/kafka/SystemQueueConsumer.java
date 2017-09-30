package com.icthh.xm.uaa.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.kafka.SystemEvent;
import com.icthh.xm.uaa.repository.util.SystemEventMapper;
import com.icthh.xm.uaa.service.UserService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SystemQueueConsumer {

    private UserService userService;

    public SystemQueueConsumer(UserService userService) {
        this.userService = userService;
    }

    /**
     * Consume tenant command event message.
     * @param message the tenant command event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MDCUtil.put();
        try {
            log.info("Input message {}", message);
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(new JavaTimeModule());
            try {
                SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);
                String command = event.getEventType();
                String userKey = String.valueOf(event.getData().get(Constants.USER_KEY));
                TenantContext.setCurrent(event.getTenantInfo());
                if (Constants.UPDATE_ACCOUNT_EVENT_TYPE.equalsIgnoreCase(command)) {
                    log.info("Start to update account for userKey='{}'", userKey);
                    User user = userService.getUser(userKey);
                    if (user == null) {
                        log.error("Failed to update account. User with userKey='{}' does not exists.", userKey);
                    } else {
                        SystemEventMapper.toUser(event, user);
                        userService.saveUser(user);
                    }
                }
            } catch (IOException e) {
                log.error("Kafka message has incorrect format ", e);
            }
        } finally {
            MDCUtil.remove();
        }
    }
}
