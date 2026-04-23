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
import java.util.Optional;
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
        UUID contractId = null;
        try {
            ContractProcessingEvent event = objectMapper.readValue(eventPayload, ContractProcessingEvent.class);
            contractId = event.getContractId();
            log.info("Worker picked up contract processing for ID: {}", contractId);

            Contract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract ID not found in DB: " + event.getContractId()));

            contract.setStatus(ContractStatus.PROCESSING);
            contractRepository.save(contract);

            // Step 1: Download from S3
            log.info("Downloading file from S3: {}", event.getS3ObjectKey());
            InputStream pdfStream = documentStorageService.downloadDocument(event.getS3ObjectKey());

            // Step 2: Extract text from PDF
            String rawText = pdfExtractionService.extractTextFromPdf(pdfStream);
            log.info("Extracted {} characters from PDF.", rawText.length());

            // Step 3: Truncate if too long for LLM context window
            if (rawText.length() > 40000) {
                log.warn("Text exceeds 40K chars, truncating to fit LLM context window");
                rawText = rawText.substring(0, 40000);
            }

            // Step 4: Send to Groq AI for structured extraction
            String jsonOutput = aiClient.extractContractDetails(rawText);

            // Step 5: Persist AI results as JSONB
            contract.setExtractedData(jsonOutput);
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
            log.info("Contract processing COMPLETED. JSONB saved to DB. ID: {}", contractId);

            // Step 6: Check for high-risk clauses and dispatch alerts
            try {
                JsonNode root = objectMapper.readTree(jsonOutput);
                if (root.has("high_risk") && root.get("high_risk").asBoolean()) {
                    String risks = root.has("risks") ? root.get("risks").toString() : "[\"Unknown risk\"]";
                    log.warn("HIGH RISK DETECTED in Contract {}. Routing alert...", contractId);
                    messageBroker.sendAlertEvent(contractId, risks);
                }
            } catch (Exception alertEx) {
                // Alert dispatch failure should NOT roll back the completed contract
                log.error("Failed to parse AI output for alert routing (non-fatal): {}", alertEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Fatal error processing contract {}: {}", contractId, e.getMessage(), e);
            markContractFailed(contractId);
        }
    }

    private void markContractFailed(UUID contractId) {
        if (contractId == null) return;
        try {
            Optional<Contract> opt = contractRepository.findById(contractId);
            opt.ifPresent(contract -> {
                contract.setStatus(ContractStatus.FAILED);
                contractRepository.save(contract);
                log.info("Marked contract {} as FAILED in database", contractId);
            });
        } catch (Exception dbEx) {
            log.error("CRITICAL: Failed to mark contract {} as FAILED: {}", contractId, dbEx.getMessage());
        }
    }
}
