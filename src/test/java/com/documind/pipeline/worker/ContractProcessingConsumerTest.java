package com.documind.pipeline.worker;

import com.documind.pipeline.ai.GroqAiClient;
import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.service.PdfExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractProcessingConsumerTest {

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
    private AlertKafkaProducer alertKafkaProducer;

    @InjectMocks
    private ContractProcessingConsumer consumer;

    private ObjectMapper realMapper;

    @BeforeEach
    void setUp() {
        realMapper = new ObjectMapper();
    }

    @Test
    void testProcessContract_Success() throws Exception {
        UUID contractId = UUID.randomUUID();
        String jsonPayload = "{\"contractId\":\"" + contractId + "\", \"s3ObjectKey\":\"key123\"}";
        
        ContractProcessingEvent event = new ContractProcessingEvent();
        event.setContractId(contractId);
        event.setS3ObjectKey("key123");

        Contract mockedContract = new Contract();
        mockedContract.setId(contractId);
        
        String extractedText = "Raw extracted PDF Text";
        String aiGeneratedJson = "{\"high_risk\": true, \"risks\": [\"Liability amount missing\"]}";

        when(objectMapper.readValue(jsonPayload, ContractProcessingEvent.class)).thenReturn(event);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockedContract));
        
        InputStream mockStream = new ByteArrayInputStream("dummy".getBytes());
        when(documentStorageService.downloadDocument("key123")).thenReturn(mockStream);
        
        when(pdfExtractionService.extractTextFromPdf(any())).thenReturn(extractedText);
        when(aiClient.extractContractDetails(extractedText)).thenReturn(aiGeneratedJson);
        
        when(objectMapper.readTree(aiGeneratedJson)).thenReturn(realMapper.readTree(aiGeneratedJson));

        // Act
        consumer.processContract(jsonPayload);

        // Assert
        verify(contractRepository, times(2)).save(mockedContract); // First for PROCESSING, then for COMPLETED
        verify(aiClient, times(1)).extractContractDetails(extractedText);
        verify(alertKafkaProducer, times(1)).sendAlertEvent(eq(contractId), anyString());
    }
}
