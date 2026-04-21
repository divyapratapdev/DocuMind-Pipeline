package com.documind.pipeline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Uploads the file to S3 and returns the S3 object key.
     */
    public String uploadDocument(MultipartFile file, UUID contractId) {
        String objectKey = contractId.toString() + "/" + file.getOriginalFilename();
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("Successfully uploaded file into S3/MinIO: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload document to S3: {}", e.getMessage());
            throw new RuntimeException("S3 document upload failure", e);
        }
    }

    /**
     * Downloads file as InputStream from S3.
     */
    public InputStream downloadDocument(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("Failed to download document from S3: {}", e.getMessage());
            throw new RuntimeException("S3 document download failure", e);
        }
    }
}
