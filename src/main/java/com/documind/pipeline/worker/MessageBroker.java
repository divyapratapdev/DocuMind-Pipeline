package com.documind.pipeline.worker;

import com.documind.pipeline.domain.ContractProcessingEvent;
import java.util.UUID;

public interface MessageBroker {
    void sendProcessingEvent(ContractProcessingEvent event);
    void sendAlertEvent(UUID contractId, String risks);
}
