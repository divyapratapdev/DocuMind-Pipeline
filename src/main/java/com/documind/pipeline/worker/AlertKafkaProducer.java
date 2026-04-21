package com.documind.pipeline.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "alert-queue";

    public void sendAlertEvent(UUID contractId, String riskDetails) {
        String payload = String.format("{\"contractId\":\"%s\", \"risks\":%s}", contractId.toString(), riskDetails);
        kafkaTemplate.send(TOPIC, contractId.toString(), payload);
        log.info("Pushed Alert Event to {} for Contract {}", TOPIC, contractId);
    }
}
