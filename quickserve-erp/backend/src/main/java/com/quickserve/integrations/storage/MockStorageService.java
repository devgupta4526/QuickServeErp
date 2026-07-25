package com.quickserve.integrations.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Mock storage service — no MinIO connection needed.
 * Files are not actually stored; returns a fake placeholder URL.
 * Active when quickserve.mock.enabled=true
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockStorageService implements StorageService {

    private static final String PLACEHOLDER_BASE = "https://picsum.photos/seed/";

    @Override
    public String upload(String bucketPath, InputStream inputStream, String contentType, long sizeBytes) {
        log.info("[MOCK STORAGE] Upload skipped — path: {}", bucketPath);
        // Return a deterministic placeholder image URL based on the path
        String seed = bucketPath.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(8, bucketPath.replaceAll("[^a-zA-Z0-9]","").length()));
        return PLACEHOLDER_BASE + seed + "/200/200";
    }

    @Override
    public String presignedUrl(String bucketPath, int expiryHours) {
        log.info("[MOCK STORAGE] Presigned URL requested for: {}", bucketPath);
        String seed = bucketPath.replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(8, bucketPath.replaceAll("[^a-zA-Z0-9]","").length()));
        return PLACEHOLDER_BASE + seed + "/400/400";
    }

    @Override
    public void delete(String bucketPath) {
        log.info("[MOCK STORAGE] Delete skipped — path: {}", bucketPath);
    }
}
