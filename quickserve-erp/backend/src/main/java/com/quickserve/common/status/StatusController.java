package com.quickserve.common.status;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class StatusController {

    private final Environment environment;
    private final boolean mockMode;

    public StatusController(Environment environment,
                            @Value("${quickserve.mock.enabled:false}") boolean mockMode) {
        this.environment = environment;
        this.mockMode = mockMode;
    }

    @GetMapping("/")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "QuickServe ERP API");
        response.put("status", "running");
        response.put("mode", mockMode ? "demo" : "production");
        response.put("profiles", Arrays.asList(environment.getActiveProfiles()));
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
