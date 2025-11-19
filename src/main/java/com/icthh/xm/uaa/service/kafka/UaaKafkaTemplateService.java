package com.icthh.xm.uaa.service.kafka;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;

/**
 * This extension can be deleted after migration to xm-commons v4.0.47 or later
 */
@Primary
@Component
public class UaaKafkaTemplateService extends KafkaTemplateService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public UaaKafkaTemplateService(KafkaTemplate<String, String> kafkaTemplate) {
        super(kafkaTemplate);
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send the data to the provided topic with headers, key and partition.
     *
     * @param topic     the topic.
     * @param data      The data.
     * @param headers   The headers that will be included in the record
     * @return a Future for the {@link SendResult}.
     **/
    @LoggingAspectConfig(inputExcludeParams = "data", resultDetails = false)
    public ListenableFuture<SendResult<String, String>> send(String topic,
                                                              String data,
                                                              Map<String, Object> headers) {
        return send(topic, null, null, data, headers);
    }

    /**
     * Send the data to the provided topic with headers and missing key or partition.
     *
     * @param topic     the topic.
     * @param partition the partition.
     * @param key       the key.
     * @param data      The data.
     * @param headers   The headers that will be included in the record
     * @return a Future for the {@link SendResult}.
     **/
    public ListenableFuture<SendResult<String, String>> send(String topic,
                                                             Integer partition,
                                                             String key,
                                                             String data,
                                                             Map<String, Object> headers) {
        MessageBuilder<String> builder = MessageBuilder
            .withPayload(data)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .setHeader(KafkaHeaders.MESSAGE_KEY, key);

        if (partition != null) {
            builder.setHeader(KafkaHeaders.PARTITION_ID, partition);
        }

        if (headers != null) {
            builder.copyHeaders(headers);
        }

        Message<String> message = builder.build();
        return kafkaTemplate.send(message);
    }
}
