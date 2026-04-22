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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractProcessingWorker {

    private final ObjectMapper objectMapper;
    private final DocumentStorageService documentStorageService;
    private final PdfExtractionService pdfExtractionService;
    private final GroqAiClient aiClient;
    private final ContractRepository contractRepository;
    private final MessageBroker messageBroker;

    public void processContract(String eventPayload) {
        try {
            ContractProcessingEvent event = objectMapper.readValue(eventPayload, ContractProcessingEvent.class);
            UUID contractId = event.getContractId();
            log.info("Worker picked up contract processing for ID: {}", contractId);

            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract ID not found in DB: " + contractId));

            contract.setStatus(ContractStatus.PROCESSING);
            contractRepository.save(contract);

            log.info("Downloading file from S3: {}", event.getS3ObjectKey());
            InputStream pdfStream = documentStorageService.downloadDocument(event.getS3ObjectKey());

            String rawText = pdfExtractionService.extractTextFromPdf(pdfStream);
            log.info("Extracted {} characters from PDF.", rawText.length());
            
            if (rawText.length() > 40000) {
                rawText = rawText.substring(0, 40000);
            }

            String jsonOutput = aiClient.extractContractDetails(rawText);

            contract.setExtractedData(jsonOutput);
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
            log.info("Contract processing successfully completed and JSONB loaded! ID: {}", contractId);

            JsonNode root = objectMapper.readTree(jsonOutput);
            if (root.has("high_risk") && root.get("high_risk").asBoolean()) {
                log.warn("HIGH RISK DETECTED in Contract {}. Routing event to Legal Team stream...", contractId);
                messageBroker.sendAlertEvent(contractId, root.get("risks").toString());
            }

        } catch (Exception e) {
            log.error("Fatal error inside processing worker: {}", e.getMessage(), e);
        }
    }
}
