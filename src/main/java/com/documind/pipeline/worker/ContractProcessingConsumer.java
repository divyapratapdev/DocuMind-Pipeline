package com.documind.pipeline.worker;

import com.documind.pipeline.ai.GroqAiClient;
import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.service.PdfExtractionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractProcessingConsumer {

    private final ObjectMapper objectMapper;
    private final DocumentStorageService documentStorageService;
    private final PdfExtractionService pdfExtractionService;
    private final GroqAiClient aiClient;
    private final ContractRepository contractRepository;
    private final AlertKafkaProducer alertKafkaProducer; // Assume we have one for alerts

    @KafkaListener(topics = "contract-processing-queue", groupId = "documind-workers")
    public void processContract(String eventPayload) {
        try {
            ContractProcessingEvent event = objectMapper.readValue(eventPayload, ContractProcessingEvent.class);
            UUID contractId = event.getContractId();
            log.info("Kafka Consumer picked up contract processing for ID: {}", contractId);

            // 1. Fetch DB record
            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract ID not found in DB: " + contractId));

            // Mark as processing
            contract.setStatus(ContractStatus.PROCESSING);
            contractRepository.save(contract);

            // 2. Load PDF document from S3
            log.info("Downloading file from S3: {}", event.getS3ObjectKey());
            InputStream pdfStream = documentStorageService.downloadDocument(event.getS3ObjectKey());

            // 3. Extract text from PDF file
            String rawText = pdfExtractionService.extractTextFromPdf(pdfStream);
            log.info("Extracted {} characters from PDF.", rawText.length());
            
            // Limit text if necessary or implement chunking (For MVP, limit to roughly 8k words ~ 40k chars)
            if (rawText.length() > 40000) {
                rawText = rawText.substring(0, 40000);
            }

            // 4. Send to LLM
            String jsonOutput = aiClient.extractContractDetails(rawText);

            // 5. Save back to DB
            contract.setExtractedData(jsonOutput);
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
            log.info("Contract processing successfully completed and JSONB loaded! ID: {}", contractId);

            // 6. Alert logic if high-risk
            JsonNode root = objectMapper.readTree(jsonOutput);
            if (root.has("high_risk") && root.get("high_risk").asBoolean()) {
                log.warn("HIGH RISK DETECTED in Contract {}. Routing event to Legal Team stream...", contractId);
                alertKafkaProducer.sendAlertEvent(contractId, root.get("risks").toString());
            }

        } catch (Exception e) {
            log.error("Fatal error inside worker consumer: {}", e.getMessage(), e);
            // In a real system, you'd send to a Dead Letter Queue (DLQ) here and mark DB as FAILED.
        }
    }
}
