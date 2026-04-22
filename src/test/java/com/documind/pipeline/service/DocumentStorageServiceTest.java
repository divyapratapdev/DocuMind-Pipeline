package com.documind.pipeline.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DocumentStorageService documentStorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentStorageService, "bucketName", bucketName);
    }

    @Test
    void testUploadDocument_Success() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "dummy data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String resultKey = documentStorageService.uploadDocument(file, contractId);

        // Assert
        assertEquals(contractId.toString() + "/contract.pdf", resultKey);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
    
    @Test
    void testUploadDocument_Failure() {
        UUID contractId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "dummy data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 is down"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> documentStorageService.uploadDocument(file, contractId));
    }
}
