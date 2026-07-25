package com.quickserve.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * No-op Kafka publisher for mock profile.
 * Logs events instead of publishing to a real broker.
 * Spring will use this bean instead of the real KafkaTemplate
 * because autoconfigure.exclude disables Kafka in mock profile.
 *
 * We achieve this by wrapping KafkaTemplate usage in an EventPublisher
 * abstraction so callers don't need to change.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockEventPublisher {

    /**
     * Drop-in replacement called by services that check the mock flag.
     */
    public void publish(String topic, String key, Object payload) {
        log.info("[MOCK KAFKA] Topic: {} | Key: {} | Payload: {}", topic, key, payload);
    }
}
