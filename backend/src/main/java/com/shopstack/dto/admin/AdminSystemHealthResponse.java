package com.shopstack.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AdminSystemHealthResponse — safe operational health metrics for the admin console.
 * Strictly exposes non-sensitive runtime and database health metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSystemHealthResponse {
    private String applicationStatus;
    private String databaseStatus;
    private String overallStatus;
    private String environment;
    private String javaVersion;
    private long uptimeSeconds;
    private long totalMemoryBytes;
    private long freeMemoryBytes;
    private long usedMemoryBytes;
    private int activeThreads;
    private LocalDateTime timestamp;
}
