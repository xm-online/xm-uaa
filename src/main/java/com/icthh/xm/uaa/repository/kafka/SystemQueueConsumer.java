package com.icthh.xm.uaa.repository.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.kafka.SystemEvent;
import com.icthh.xm.uaa.repository.util.SystemEventMapper;
import com.icthh.xm.uaa.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;

@RequiredArgsConstructor
@Slf4j
@Service
@IgnoreLogginAspect
public class SystemQueueConsumer {

    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder authContextHolder;
    private final LepManager lepManager;
    private final UserService userService;

    /**
     * Consume tenant command event message.
     *
     * @param message the tenant command event message
     */
    @Retryable(maxAttemptsExpression = "${application.retry.max-attempts}",
        backoff = @Backoff(delayExpression = "${application.retry.delay}",
            multiplierExpression = "${application.retry.multiplier}"))
    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
            log.info("Consume event from topic [{}]", message.topic());
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(new JavaTimeModule());
            try {
                SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);

                log.info("Process event from topic [{}], type='{}', source='{}', event_id ='{}'",
                    message.topic(), event.getEventType(), event.getMessageSource(), event.getEventId());

                switch (event.getEventType().toUpperCase()) {
                    case Constants.UPDATE_ACCOUNT_EVENT_TYPE:
                        onUpdateAcount(event);
                        break;
                    default:
                        log.info("Event ignored with type='{}', source='{}', event_id='{}'",
                            event.getEventType(), event.getMessageSource(), event.getEventId());
                        break;
                }
            } catch (IOException e) {
                log.error("System queue message has incorrect format: '{}'", message.value(), e);
            }
        } finally {
            destroy();
        }
    }

    private void init(String tenantKey, String login) {
        if (StringUtils.isNotBlank(tenantKey)) {
            TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

            lepManager.beginThreadContext(threadContext -> {
                threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
                threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
            });
        }

        String newRid = MdcUtils.getRid()
            + ":" + StringUtils.defaultIfBlank(login, "")
            + ":" + StringUtils.defaultIfBlank(tenantKey, "");
        MdcUtils.putRid(newRid);
    }

    private void destroy() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        MdcUtils.removeRid();
    }

    private void onUpdateAcount(SystemEvent event) {
        String userKey = String.valueOf(event.getDataMap().get(Constants.SYSTEM_EVENT_PROP_USER_KEY));

        init(event.getTenantKey(), event.getUserLogin());

        log.info("Start to update account for userKey='{}'", userKey);
        User user = userService.getUser(userKey);
        if (user == null) {
            log.error("Failed to update account. User with userKey='{}' does not exists.", userKey);
        } else {
            SystemEventMapper.toUser(event, user);
            userService.saveUser(user);
        }
    }
}
