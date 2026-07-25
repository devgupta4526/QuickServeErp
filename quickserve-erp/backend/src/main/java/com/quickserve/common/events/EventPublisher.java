package com.quickserve.common.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * EventPublisher abstracts Kafka publishing.
 * In mock mode (quickserve.mock.enabled=true) it logs events instead.
 * In production it uses the real KafkaTemplate.
 */
@Slf4j
@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean mockEnabled;

    @Autowired
    public EventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @org.springframework.beans.factory.annotation.Value("${quickserve.mock.enabled:false}") boolean mockEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.mockEnabled   = mockEnabled;
    }

    public void publish(String topic, String key, Object payload) {
        if (mockEnabled || kafkaTemplate == null) {
            log.info("[MOCK EVENT] topic={} key={} payload={}", topic, key, payload);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.error("Kafka publish failed — topic={} key={}: {}", topic, key, e.getMessage());
        }
    }

    public void publish(String topic, Object payload) {
        publish(topic, null, payload);
    }
}
