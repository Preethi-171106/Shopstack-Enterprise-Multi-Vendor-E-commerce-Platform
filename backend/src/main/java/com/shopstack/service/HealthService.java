package com.shopstack.service;

import com.shopstack.dto.HealthResponse;
import com.shopstack.dto.admin.AdminSystemHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;

/**
 * HealthService — Service layer for public and administrative health monitoring.
 * Ensures zero exposure of credentials, passwords, or secret keys.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns the basic health status for public ping.
     */
    public HealthResponse getHealthStatus() {
        return new HealthResponse("UP", "ShopStack backend is running");
    }

    /**
     * Returns operational system health and DB connectivity metrics for ADMIN role.
     */
    public AdminSystemHealthResponse getAdminSystemHealth() {
        String dbStatus = "DOWN";
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                dbStatus = "UP";
            }
        } catch (Exception e) {
            log.error("[HealthService] Database health check failed: {}", e.getMessage());
            dbStatus = "DOWN";
        }

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        int activeThreads = Thread.activeCount();
        String javaVersion = System.getProperty("java.version");

        String overallStatus = "UP".equalsIgnoreCase(dbStatus) ? "UP" : "DEGRADED";

        return AdminSystemHealthResponse.builder()
                .applicationStatus("UP")
                .databaseStatus(dbStatus)
                .overallStatus(overallStatus)
                .environment("Production-Ready")
                .javaVersion(javaVersion)
                .uptimeSeconds(uptimeSeconds)
                .totalMemoryBytes(totalMemory)
                .freeMemoryBytes(freeMemory)
                .usedMemoryBytes(usedMemory)
                .activeThreads(activeThreads)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
