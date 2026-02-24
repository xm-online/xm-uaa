package com.icthh.xm.uaa.config.testcontainer;

import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;

public interface KafkaTestContainer {

    @Container
    KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:4.1.1")
        .withStartupTimeout(Duration.ofMinutes(2))
        .withStartupAttempts(3)
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaTestContainer.class)));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.cloud.stream.kafka.binder.brokers",
            () -> KAFKA_CONTAINER.getHost() + ':' + KAFKA_CONTAINER.getFirstMappedPort()
        );
    }
}
