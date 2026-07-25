package com.quickserve.integrations.storage;

import java.io.InputStream;

/**
 * Abstraction for file storage.
 * Production: MinioStorageService
 * Mock/dev:   MockStorageService (returns fake URLs, stores nothing)
 */
public interface StorageService {

    /**
     * Upload a file and return its public/presigned URL.
     *
     * @param bucketPath  path within bucket, e.g. "business-assets/uuid/logo.png"
     * @param inputStream file content
     * @param contentType MIME type
     * @param sizeBytes   file size in bytes (-1 if unknown)
     * @return            accessible URL for the uploaded file
     */
    String upload(String bucketPath, InputStream inputStream, String contentType, long sizeBytes);

    /**
     * Generate a presigned URL valid for the given number of hours.
     */
    String presignedUrl(String bucketPath, int expiryHours);

    /**
     * Delete a file.
     */
    void delete(String bucketPath);
}
