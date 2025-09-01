package com.icthh.xm.uaa.listener;

import com.icthh.xm.commons.domainevent.db.service.kafka.SystemQueueConsumer;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.domain.EnvironmentVariable;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.health_check.KafkaTopicsHealthIndicator;
import com.icthh.xm.uaa.repository.kafka.SystemTopicConsumer;
import com.icthh.xm.uaa.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationProperties applicationProperties;
    private final ConsumerFactory<String, String> consumerFactory;
    private final SystemQueueConsumer systemQueueConsumer;
    private final SystemTopicConsumer systemTopicConsumer;
    private final Environment env;
    private final KafkaProperties kafkaProperties;
    private final EnvironmentService environmentService;
    private final KafkaTopicsHealthIndicator kafkaTopicsHealthIndicator;

    private final PrivilegeInspector privilegeInspector;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (applicationProperties.isKafkaEnabled()) {
            waitForKafkaTopicsReady();
            createKafkaConsumers();
            privilegeInspector.readPrivileges(MdcUtils.getRid());
        } else {
            log.warn("WARNING! Privileges inspection is disabled by "
                + "configuration parameter 'application.kafka-enabled'");
        }

        updateEnvironmentListForPermissions();
    }

    private void waitForKafkaTopicsReady() {
        int retries = applicationProperties.getRetry().getMaxAttempts();
        long delayMs = applicationProperties.getRetry().getDelay();

        for (int i = 0; i < retries; i++) {
            final Health health = kafkaTopicsHealthIndicator.health();
            if (health.getStatus().equals(Status.UP)) {
                log.info("Kafka topics are ready.");
                return;
            }
            log.warn("Kafka topics not ready yet, attempt {}/{}", i + 1, retries);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Kafka topics", e);
            }
        }
        throw new IllegalStateException("Kafka topics are not ready after retries");
    }

    private void updateEnvironmentListForPermissions() {
        List<String> envVars = Arrays.stream(EnvironmentVariable.values())
            .map(EnvironmentVariable::getName)
            .collect(Collectors.toList());

        environmentService.updateConfigs(envVars);

    }

    private void createKafkaConsumers() {
        createSystemConsumer(applicationProperties.getKafkaSystemTopic(), systemTopicConsumer::consumeEvent);
        createSystemConsumer(applicationProperties.getKafkaSystemQueue(), systemQueueConsumer::consumeEvent);
    }

    private void createSystemConsumer(String name, MessageListener<String, String> consumeEvent) {
        log.info("Creating kafka consumer for topic {}", name);
        ContainerProperties containerProps = new ContainerProperties(name);

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        if (name.equals(applicationProperties.getKafkaSystemTopic())) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        }
        ConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);

        ConcurrentMessageListenerContainer<String, String> container =
            new ConcurrentMessageListenerContainer<>(factory, containerProps);
        container.setupMessageListener(consumeEvent);
        container.start();
        log.info("Successfully created kafka consumer for topic {}", name);
    }

}
