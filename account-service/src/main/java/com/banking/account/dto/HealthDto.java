package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class HealthDto {

    /**
     * Response DTO — for checking service health.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String serviceName;
        private String version;
        private serviceStatus status;

        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
    }

    public enum serviceStatus {
        ACTIVE,
        INACTIVE
    }
}
