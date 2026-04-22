package com.documind.pipeline.worker;

import com.documind.pipeline.domain.ContractProcessingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.broker", havingValue = "redis")
public class RedisMessageBroker implements MessageBroker {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendProcessingEvent(ContractProcessingEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend("contract-processing-queue", payload);
            log.info("[REDIS] Sent contract processing event for ID: {}", event.getContractId());
        } catch (Exception e) {
            log.error("[REDIS] Failed to serialize or send event: {}", e.getMessage());
            throw new RuntimeException("Broker failure", e);
        }
    }

    @Override
    public void sendAlertEvent(UUID contractId, String risks) {
        try {
            redisTemplate.convertAndSend("alert-queue", contractId.toString() + "::" + risks);
            log.info("[REDIS] Sent high-risk alert for contract ID: {}", contractId);
        } catch (Exception e) {
            log.error("[REDIS] Failed to send alert: {}", e.getMessage());
        }
    }
}
