package com.documind.pipeline.api;

import com.documind.pipeline.domain.Contract;
import com.documind.pipeline.domain.ContractProcessingEvent;
import com.documind.pipeline.domain.ContractRepository;
import com.documind.pipeline.domain.ContractStatus;
import com.documind.pipeline.service.DocumentStorageService;
import com.documind.pipeline.worker.MessageBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractControllerTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private MessageBroker messageBroker;

    @InjectMocks
    private ContractController contractController;

    @Test
    void testUploadContract_Success() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Dummy PDF Content".getBytes()
        );

        Contract mockContract = Contract.builder()
                .id(contractId)
                .organizationId(orgId)
                .status(ContractStatus.PENDING)
                .build();

        when(contractRepository.save(any(Contract.class))).thenReturn(mockContract);
        when(documentStorageService.uploadDocument(any(), any())).thenReturn(contractId + "/test-document.pdf");

        // Act
        ResponseEntity<Map<String, Object>> response = contractController.uploadContract(mockFile, orgId);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PENDING", response.getBody().get("status"));
        assertEquals(contractId.toString(), response.getBody().get("contractId"));

        // Verify DB Save occurred twice (initial + update with s3 key)
        verify(contractRepository, times(2)).save(any(Contract.class));
        verify(documentStorageService, times(1)).uploadDocument(mockFile, contractId);

        // Verify message broker event pushed correctly
        ArgumentCaptor<ContractProcessingEvent> eventCaptor = ArgumentCaptor.forClass(ContractProcessingEvent.class);
        verify(messageBroker, times(1)).sendProcessingEvent(eventCaptor.capture());

        ContractProcessingEvent capturedEvent = eventCaptor.getValue();
        assertEquals(contractId, capturedEvent.getContractId());
        assertEquals(orgId, capturedEvent.getOrganizationId());
        assertEquals(contractId + "/test-document.pdf", capturedEvent.getS3ObjectKey());
    }

    @Test
    void testUploadContract_EmptyFile_Returns400() {
        UUID orgId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        ResponseEntity<Map<String, Object>> response = contractController.uploadContract(emptyFile, orgId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", response.getBody().get("error"));
        verify(contractRepository, never()).save(any());
    }

    @Test
    void testUploadContract_NonPdfFile_Returns400() {
        UUID orgId = UUID.randomUUID();
        MockMultipartFile txtFile = new MockMultipartFile("file", "document.txt", "text/plain", "text content".getBytes());

        ResponseEntity<Map<String, Object>> response = contractController.uploadContract(txtFile, orgId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Only PDF files are accepted", response.getBody().get("error"));
        verify(contractRepository, never()).save(any());
    }
}
