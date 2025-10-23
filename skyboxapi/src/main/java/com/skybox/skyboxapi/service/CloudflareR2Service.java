package com.skybox.skyboxapi.service;

import com.skybox.skyboxapi.config.CloudflareR2Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudflareR2Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CloudflareR2Config r2Config;

    /**
     * Upload a file to Cloudflare R2
     * @param file The file to upload
     * @param folder The folder path (e.g., "public" or "private/userId")
     * @return The key (path) of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        log.info("File uploaded successfully to R2: {}", key);
        return key;
    }

    /**
     * Download a file from Cloudflare R2
     * @param key The key (path) of the file
     * @return The file content as byte array
     */
    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .build();

        try {
            return s3Client.getObject(getObjectRequest).readAllBytes();
        } catch (Exception e) {
            log.error("Error downloading file from R2: {}", key, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    /**
     * Delete a file from Cloudflare R2
     * @param key The key (path) of the file to delete
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("File deleted from R2: {}", key);
    }

    /**
     * Generate a presigned URL for temporary access to a private file
     * @param key The key (path) of the file
     * @param duration How long the URL should be valid
     * @return Presigned URL string
     */
    public String getPresignedUrl(String key, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Get the public URL for a file (if custom domain is configured)
     * @param key The key (path) of the file
     * @return Public URL string
     */
    public String getPublicUrl(String key) {
        if (r2Config.getPublicDomain() != null && !r2Config.getPublicDomain().isEmpty()) {
            return r2Config.getPublicDomain() + "/" + key;
        }
        // Fallback to R2 endpoint
        return r2Config.getEndpoint() + "/" + r2Config.getBucketName() + "/" + key;
    }

    /**
     * Generate a unique filename with the original extension
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Check if a file exists in R2
     * @param key The key (path) of the file
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
