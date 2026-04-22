package com.documind.pipeline.worker;

import com.documind.pipeline.domain.ContractProcessingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.broker", havingValue = "kafka", matchIfMissing = true)
public class KafkaMessageBroker implements MessageBroker {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendProcessingEvent(ContractProcessingEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("contract-processing-queue", event.getContractId().toString(), payload);
            log.info("[KAFKA] Sent contract processing event for ID: {}", event.getContractId());
        } catch (Exception e) {
            log.error("[KAFKA] Failed to serialize or send event: {}", e.getMessage());
            throw new RuntimeException("Broker failure", e);
        }
    }

    @Override
    public void sendAlertEvent(UUID contractId, String risks) {
        try {
            kafkaTemplate.send("alert-queue", contractId.toString(), risks);
            log.info("[KAFKA] Sent high-risk alert for contract ID: {}", contractId);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to send alert: {}", e.getMessage());
        }
    }
}
