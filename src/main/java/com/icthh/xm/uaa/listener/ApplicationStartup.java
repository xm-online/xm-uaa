package com.icthh.xm.uaa.listener;

import com.icthh.xm.commons.domainevent.db.service.kafka.SystemQueueConsumer;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.domain.EnvironmentVariable;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.repository.kafka.SystemTopicConsumer;
import com.icthh.xm.uaa.service.EnvironmentService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
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

    private final PrivilegeInspector privilegeInspector;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (applicationProperties.isKafkaEnabled()) {
            createKafkaConsumers();
            privilegeInspector.readPrivileges(MdcUtils.getRid());
        } else {
            log.warn("WARNING! Privileges inspection is disabled by "
                    + "configuration parameter 'application.kafka-enabled'");
        }

        updateEnvironmentListForPermissions();
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
        containerProps.setMissingTopicsFatal(false); // do not throw exception if topic is not found

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
