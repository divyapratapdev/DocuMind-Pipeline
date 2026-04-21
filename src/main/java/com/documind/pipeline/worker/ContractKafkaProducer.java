package com.documind.pipeline.worker;

import com.documind.pipeline.domain.ContractProcessingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "contract-processing-queue";

    public void sendProcessingEvent(ContractProcessingEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getContractId().toString(), payload);
            log.info("Successfully pushed processing event to Kafka topic: [{}] with payload: {}", TOPIC, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka event: {}", e.getMessage());
            throw new RuntimeException("Kafka serialization failure", e);
        }
    }
}
