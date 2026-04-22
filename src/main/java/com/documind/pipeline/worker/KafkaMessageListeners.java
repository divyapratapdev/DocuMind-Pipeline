package com.documind.pipeline.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.broker", havingValue = "kafka", matchIfMissing = true)
public class KafkaMessageListeners {

    private final ContractProcessingWorker processingWorker;

    @KafkaListener(topics = "contract-processing-queue", groupId = "documind-workers")
    public void onContractEvent(String payload) {
        log.info("[KAFKA LISTENER] Received event on contract-processing-queue");
        processingWorker.processContract(payload);
    }

    @KafkaListener(topics = "alert-queue", groupId = "legal-team-workers")
    public void onAlertEvent(String risks) {
        log.error("[KAFKA LISTENER] URGENT LEGAL ALERT: Escalate Contract immediately! Risks: {}", risks);
    }
}
