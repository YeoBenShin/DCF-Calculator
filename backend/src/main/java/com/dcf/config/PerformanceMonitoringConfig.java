package com.dcf.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for BigDecimal performance monitoring
 * Enables scheduled tasks for periodic performance reporting
 */
@Configuration
@EnableScheduling
public class PerformanceMonitoringConfig {
    // Configuration class to enable scheduling for performance monitoring
    // The @EnableScheduling annotation allows the BigDecimalPerformanceMonitoringService
    // to run its scheduled performance reports every 5 minutes
}