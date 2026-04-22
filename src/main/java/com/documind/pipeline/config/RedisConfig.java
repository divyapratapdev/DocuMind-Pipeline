package com.documind.pipeline.config;

import com.documind.pipeline.worker.ContractProcessingWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "messaging.broker", havingValue = "redis")
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter processingListener,
                                            MessageListenerAdapter alertListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(processingListener, new ChannelTopic("contract-processing-queue"));
        container.addMessageListener(alertListener, new ChannelTopic("alert-queue"));
        log.info("[REDIS] Registered Pub/Sub Listeners for contract processing and alerts");
        return container;
    }

    @Bean
    MessageListenerAdapter processingListener(ContractProcessingWorker worker) {
        return new MessageListenerAdapter(new ProcessingMessageDelegate(worker), "handleMessage");
    }

    @Bean
    MessageListenerAdapter alertListener() {
        return new MessageListenerAdapter(new AlertMessageDelegate(), "handleMessage");
    }

    public static class ProcessingMessageDelegate {
        private final ContractProcessingWorker worker;
        public ProcessingMessageDelegate(ContractProcessingWorker worker) { this.worker = worker; }
        public void handleMessage(String message) {
            log.info("[REDIS LISTENER] Received event on contract-processing-queue");
            worker.processContract(message);
        }
    }

    public static class AlertMessageDelegate {
        public void handleMessage(String message) {
            log.error("[REDIS LISTENER] URGENT LEGAL ALERT: Escalate Contract immediately! Risks: {}", message);
        }
    }
}
