package com.documind.pipeline.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStorageServiceTest {

    @Test
    void testUploadAndDownload() throws Exception {
        DocumentStorageService service = new DocumentStorageService();
        UUID contractId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        String objectKey = service.uploadDocument(file, contractId);
        assertNotNull(objectKey);
        assertTrue(objectKey.contains("test.pdf"));

        InputStream is = service.downloadDocument(objectKey);
        assertNotNull(is);
    }
}
