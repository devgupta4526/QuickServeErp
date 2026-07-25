package com.quickserve.integrations.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Real MinIO storage service — active when quickserve.mock.enabled is false or absent.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${quickserve.minio.bucket-name:quickserve}")
    private String bucketName;

    @Value("${quickserve.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Override
    public String upload(String bucketPath, InputStream inputStream, String contentType, long sizeBytes) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(bucketPath)
                    .stream(inputStream, sizeBytes, -1)
                    .contentType(contentType)
                    .build());
            return endpoint + "/" + bucketName + "/" + bucketPath;
        } catch (Exception e) {
            log.error("MinIO upload failed for {}: {}", bucketPath, e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public String presignedUrl(String bucketPath, int expiryHours) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(bucketPath)
                    .method(Method.GET)
                    .expiry(expiryHours, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Presigned URL failed for {}: {}", bucketPath, e.getMessage());
            return endpoint + "/" + bucketName + "/" + bucketPath;
        }
    }

    @Override
    public void delete(String bucketPath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(bucketPath)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO delete failed for {}: {}", bucketPath, e.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}
