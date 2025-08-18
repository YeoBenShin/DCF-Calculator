package com.dcf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance profiler for BigDecimal operations
 * Tracks execution times and operation counts for BigDecimal-heavy calculations
 */
@Component
public class BigDecimalPerformanceProfiler {

    private static final Logger logger = LoggerFactory.getLogger(BigDecimalPerformanceProfiler.class);

    private final ConcurrentMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();
    private final ThreadLocal<ProfileContext> currentContext = new ThreadLocal<>();

    /**
     * Start profiling an operation
     * @param operationName the name of the operation being profiled
     * @return ProfileContext for the operation
     */
    public ProfileContext startProfiling(String operationName) {
        ProfileContext context = new ProfileContext(operationName, Instant.now());
        currentContext.set(context);
        return context;
    }

    /**
     * End profiling and record the results
     * @param context the profile context from startProfiling
     */
    public void endProfiling(ProfileContext context) {
        if (context == null) {
            logger.warn("Attempted to end profiling with null context");
            return;
        }

        Duration duration = Duration.between(context.getStartTime(), Instant.now());
        recordOperation(context.getOperationName(), duration);
        currentContext.remove();
    }

    /**
     * Profile a BigDecimal operation with automatic timing
     * @param operationName the name of the operation
     * @param operation the operation to profile
     * @param <T> the return type
     * @return the result of the operation
     */
    public <T> T profileOperation(String operationName, BigDecimalOperation<T> operation) {
        ProfileContext context = startProfiling(operationName);
        try {
            return operation.execute();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        } finally {
            endProfiling(context);
        }
    }

    /**
     * Record BigDecimal arithmetic operation performance
     * @param operationType the type of arithmetic operation (add, subtract, multiply, divide, pow)
     * @param operandCount the number of operands involved
     * @param precision the precision of the operation
     * @param duration the execution duration
     */
    public void recordArithmeticOperation(String operationType, int operandCount, int precision, Duration duration) {
        String key = String.format("arithmetic_%s_operands_%d_precision_%d", operationType, operandCount, precision);
        recordOperation(key, duration);
    }

    /**
     * Record a timed operation
     * @param operationName the operation name
     * @param duration the execution duration
     */
    private void recordOperation(String operationName, Duration duration) {
        operationStats.computeIfAbsent(operationName, k -> new OperationStats())
                     .recordExecution(duration);
        
        // Log slow operations (> 100ms)
        if (duration.toMillis() > 100) {
            logger.warn("Slow BigDecimal operation detected: {} took {}ms", operationName, duration.toMillis());
        }
    }

    /**
     * Get performance statistics for all operations
     * @return map of operation names to their statistics
     */
    public ConcurrentMap<String, OperationStats> getPerformanceStats() {
        return new ConcurrentHashMap<>(operationStats);
    }

    /**
     * Get performance statistics for a specific operation
     * @param operationName the operation name
     * @return operation statistics or null if not found
     */
    public OperationStats getOperationStats(String operationName) {
        return operationStats.get(operationName);
    }

    /**
     * Clear all performance statistics
     */
    public void clearStats() {
        operationStats.clear();
        logger.info("Cleared all BigDecimal performance statistics");
    }

    /**
     * Get performance summary report
     * @return formatted performance report
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("BigDecimal Performance Report:\n");
        report.append("============================\n");

        operationStats.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().getTotalExecutionTime(), e1.getValue().getTotalExecutionTime()))
                .forEach(entry -> {
                    String operationName = entry.getKey();
                    OperationStats stats = entry.getValue();
                    report.append(String.format("Operation: %s\n", operationName));
                    report.append(String.format("  Executions: %d\n", stats.getExecutionCount()));
                    report.append(String.format("  Total Time: %dms\n", stats.getTotalExecutionTime()));
                    report.append(String.format("  Average Time: %.2fms\n", stats.getAverageExecutionTime()));
                    report.append(String.format("  Min Time: %dms\n", stats.getMinExecutionTime()));
                    report.append(String.format("  Max Time: %dms\n", stats.getMaxExecutionTime()));
                    report.append("\n");
                });

        return report.toString();
    }

    /**
     * Profile context for tracking operation execution
     */
    public static class ProfileContext {
        private final String operationName;
        private final Instant startTime;

        public ProfileContext(String operationName, Instant startTime) {
            this.operationName = operationName;
            this.startTime = startTime;
        }

        public String getOperationName() {
            return operationName;
        }

        public Instant getStartTime() {
            return startTime;
        }
    }

    /**
     * Statistics for a specific operation
     */
    public static class OperationStats {
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = Long.MIN_VALUE;

        public void recordExecution(Duration duration) {
            long durationMs = duration.toMillis();
            executionCount.incrementAndGet();
            totalExecutionTime.addAndGet(durationMs);
            
            // Update min/max with thread-safe approach
            updateMinTime(durationMs);
            updateMaxTime(durationMs);
        }

        private synchronized void updateMinTime(long durationMs) {
            if (durationMs < minExecutionTime) {
                minExecutionTime = durationMs;
            }
        }

        private synchronized void updateMaxTime(long durationMs) {
            if (durationMs > maxExecutionTime) {
                maxExecutionTime = durationMs;
            }
        }

        public long getExecutionCount() {
            return executionCount.get();
        }

        public long getTotalExecutionTime() {
            return totalExecutionTime.get();
        }

        public double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }

        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }

        public long getMaxExecutionTime() {
            return maxExecutionTime == Long.MIN_VALUE ? 0 : maxExecutionTime;
        }
    }

    /**
     * Functional interface for BigDecimal operations
     */
    @FunctionalInterface
    public interface BigDecimalOperation<T> {
        T execute() throws Exception;
    }
}