package com.quickserve.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${quickserve.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${quickserve.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${quickserve.minio.secret-key:minioadmin}")
    private String secretKey;

    /**
     * Only create the real MinioClient when NOT in mock mode.
     * In mock mode the MockStorageService is used instead and needs no MinIO connection.
     */
    @Bean
    @ConditionalOnProperty(name = "quickserve.mock.enabled", havingValue = "false", matchIfMissing = true)
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
