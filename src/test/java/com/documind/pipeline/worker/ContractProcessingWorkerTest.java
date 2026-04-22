package com.documind.pipeline.worker;

import com.documind.pipeline.ai.GroqAiClient;
import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.service.PdfExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContractProcessingWorkerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private PdfExtractionService pdfExtractionService;
    @Mock
    private GroqAiClient aiClient;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private MessageBroker messageBroker;

    @InjectMocks
    private ContractProcessingWorker worker;

    @Test
    void testProcessContract_Success() throws Exception {
        UUID contractId = UUID.randomUUID();
        ContractProcessingEvent event = ContractProcessingEvent.builder()
                .contractId(contractId)
                .s3ObjectKey("key123")
                .build();
        
        String payload = "dummy_payload";
        
        when(objectMapper.readValue(payload, ContractProcessingEvent.class)).thenReturn(event);
        
        Contract dbContract = new Contract();
        dbContract.setId(contractId);
        
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(dbContract));
        when(documentStorageService.downloadDocument("key123")).thenReturn(mock(InputStream.class));
        when(pdfExtractionService.extractTextFromPdf(any())).thenReturn("Raw PDF Text...");
        
        String mockJson = "{\"high_risk\": true, \"risks\": [\"clause1\"]}";
        when(aiClient.extractContractDetails(anyString())).thenReturn(mockJson);
        
        when(objectMapper.readTree(mockJson)).thenReturn(new ObjectMapper().readTree(mockJson));

        worker.processContract(payload);

        verify(contractRepository, times(2)).save(dbContract);
        verify(messageBroker, times(1)).sendAlertEvent(eq(contractId), anyString());
    }
}
