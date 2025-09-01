package com.icthh.xm.uaa.listener;

import com.icthh.xm.commons.domainevent.db.service.kafka.SystemQueueConsumer;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.domain.EnvironmentVariable;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.repository.kafka.SystemTopicConsumer;
import com.icthh.xm.uaa.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
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
        systemTopicConsumer.createSystemConsumer(applicationProperties.getKafkaSystemTopic(), systemTopicConsumer::consumeEvent);
        systemTopicConsumer.createSystemConsumer(applicationProperties.getKafkaSystemQueue(), systemQueueConsumer::consumeEvent);
    }

}
