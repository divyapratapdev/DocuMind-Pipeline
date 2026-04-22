package com.documind.pipeline.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class DocumentStorageService {

    private final Path storageDir = Paths.get("/tmp/documind-contracts");

    public DocumentStorageService() {
        try {
            Files.createDirectories(storageDir);
        } catch (Exception e) {
            log.error("Could not initialize storage directory", e);
        }
    }

    public String uploadDocument(MultipartFile file, UUID contractId) {
        String objectKey = contractId.toString() + "_" + file.getOriginalFilename();
        Path targetPath = storageDir.resolve(objectKey);
        
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved file to local ephemeral storage: {}", targetPath);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to save document locally: {}", e.getMessage());
            throw new RuntimeException("Local document upload failure", e);
        }
    }

    public InputStream downloadDocument(String objectKey) {
        try {
            Path targetPath = storageDir.resolve(objectKey);
            return Files.newInputStream(targetPath);
        } catch (Exception e) {
            log.error("Failed to read document from local storage: {}", e.getMessage());
            throw new RuntimeException("Local document download failure", e);
        }
    }
}
