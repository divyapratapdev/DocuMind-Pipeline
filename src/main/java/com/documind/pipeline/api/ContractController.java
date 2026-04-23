package com.documind.pipeline.api;

import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.worker.MessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractRepository contractRepository;
    private final DocumentStorageService documentStorageService;
    private final MessageBroker messageBroker;

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("organizationId") UUID organizationId) {

        log.info("Received contract upload request. original filename: {}", file.getOriginalFilename());
        
        // 1. Initial Insert into Postgres (PENDING logic)
        Contract initialContract = Contract.builder()
                .organizationId(organizationId)
                .filename(file.getOriginalFilename()) // temporary raw filename
                .uploadTimestamp(LocalDateTime.now())
                .status(ContractStatus.PENDING)
                .build();
        
        initialContract = contractRepository.save(initialContract);

        // 2. Upload file to MinIO storage synchronously
        String s3Key = documentStorageService.uploadDocument(file, initialContract.getId());
        
        // Update entity with actual storage path
        initialContract.setFilename(s3Key);
        contractRepository.save(initialContract);

        // 3. Drop event on Kafka for async processing
        ContractProcessingEvent event = ContractProcessingEvent.builder()
                .contractId(initialContract.getId())
                .organizationId(organizationId)
                .s3ObjectKey(s3Key)
                .build();

        messageBroker.sendProcessingEvent(event);

        // 4. Return 202 Accepted meaning: I got it, go do whatever else. Check back later.
        log.info("Dispatched async processing event. Return 202. ID: {}", initialContract.getId());
        return ResponseEntity.accepted().body("Contract upload accepted. Processing asynchronously: " + initialContract.getId());
    }
}
