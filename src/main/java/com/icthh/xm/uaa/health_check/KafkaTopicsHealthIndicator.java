package com.icthh.xm.uaa.health_check;

import com.icthh.xm.uaa.config.ApplicationProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTopicsHealthIndicator implements HealthIndicator {

    private final ApplicationProperties applicationProperties;
    private final KafkaProperties kafkaProperties;


    @Override
    public Health health() {
        final List<String> topicsToCheck = List.of(
                applicationProperties.getKafkaSystemTopic(),
                applicationProperties.getKafkaSystemQueue()
        );

        try (AdminClient adminClient = AdminClient.create(kafkaProperties.buildConsumerProperties())) {
            DescribeTopicsResult result = adminClient.describeTopics(topicsToCheck);
            Map<String, KafkaFuture<TopicDescription>> values = result.values();

            for (String topic : topicsToCheck) {
                KafkaFuture<TopicDescription> future = values.get(topic);
                future.get();
            }

            return Health.up().withDetail("topics", topicsToCheck).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("topics", topicsToCheck).build();
        }
    }
}
