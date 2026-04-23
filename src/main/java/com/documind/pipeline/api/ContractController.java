package com.documind.pipeline.api;

import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.worker.MessageBroker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Tag(name = "Contract Controller", description = "Enterprise AI Contract Ingestion & Compliance Pipeline")
public class ContractController {

    private final ContractRepository contractRepository;
    private final DocumentStorageService documentStorageService;
    private final MessageBroker messageBroker;

    @Operation(summary = "Upload a legal contract for AI analysis",
               description = "Accepts a PDF contract file, stores it in S3, saves metadata to PostgreSQL, and dispatches an async AI extraction event via Redis Pub/Sub.")
    @ApiResponse(responseCode = "202", description = "Contract accepted for async processing")
    @ApiResponse(responseCode = "400", description = "Invalid file or missing parameters")
    @ApiResponse(responseCode = "500", description = "Internal server error during upload")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadContract(
            @Parameter(description = "PDF contract file to analyze")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Organization UUID that owns this contract")
            @RequestParam("organizationId") UUID organizationId) {

        // Edge case: empty file upload
        if (file.isEmpty()) {
            log.warn("Rejected empty file upload from organization: {}", organizationId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File is empty",
                    "status", 400
            ));
        }

        // Edge case: non-PDF file upload
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            log.warn("Rejected non-PDF file: {}", originalFilename);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Only PDF files are accepted",
                    "status", 400
            ));
        }

        log.info("Received contract upload request. original filename: {}", originalFilename);

        // 1. Initial Insert into Postgres (PENDING)
        Contract initialContract = Contract.builder()
                .organizationId(organizationId)
                .filename(originalFilename)
                .uploadTimestamp(LocalDateTime.now())
                .status(ContractStatus.PENDING)
                .build();

        initialContract = contractRepository.save(initialContract);

        // 2. Upload file to S3 storage synchronously
        String s3Key = documentStorageService.uploadDocument(file, initialContract.getId());

        // Update entity with actual storage path
        initialContract.setFilename(s3Key);
        contractRepository.save(initialContract);

        // 3. Drop event on message broker for async processing
        ContractProcessingEvent event = ContractProcessingEvent.builder()
                .contractId(initialContract.getId())
                .organizationId(organizationId)
                .s3ObjectKey(s3Key)
                .build();

        messageBroker.sendProcessingEvent(event);

        // 4. Return 202 Accepted with structured JSON response
        log.info("Dispatched async processing event. Return 202. ID: {}", initialContract.getId());
        return ResponseEntity.accepted().body(Map.of(
                "message", "Contract upload accepted. Processing asynchronously.",
                "contractId", initialContract.getId().toString(),
                "status", "PENDING",
                "filename", originalFilename
        ));
    }

    @Operation(summary = "Get contract status and AI extraction results",
               description = "Returns the full contract entity including AI-extracted JSONB data once processing is complete.")
    @ApiResponse(responseCode = "200", description = "Contract found")
    @ApiResponse(responseCode = "404", description = "Contract not found")
    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable UUID id) {
        return contractRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all contracts for an organization",
               description = "Returns all contracts belonging to the specified organization.")
    @GetMapping
    public ResponseEntity<List<Contract>> getContractsByOrganization(
            @RequestParam("organizationId") UUID organizationId) {
        List<Contract> contracts = contractRepository.findByOrganizationId(organizationId);
        return ResponseEntity.ok(contracts);
    }
}
