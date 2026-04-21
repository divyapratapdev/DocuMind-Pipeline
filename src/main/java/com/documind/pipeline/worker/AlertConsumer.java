package com.documind.pipeline.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertConsumer {

    @KafkaListener(topics = "alert-queue", groupId = "documind-alert-workers")
    public void consumeAlert(String eventPayload) {
        log.error("==================== HIGH RISK ALERT ====================");
        log.error("Legal team notified of critical risk clause:");
        log.error("Payload: {}", eventPayload);
        log.error("=========================================================");
        // In a real system, you might trigger an email via SendGrid, 
        // a Slack Webhook, or PagerDuty integration here.
    }
}
