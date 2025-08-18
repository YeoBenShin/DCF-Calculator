package com.dcf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database optimization utilities for BigDecimal operations
 * Provides optimized queries and indexing recommendations for DECIMAL columns
 */
@Component
public class BigDecimalDatabaseOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(BigDecimalDatabaseOptimizer.class);

    @PersistenceContext
    private EntityManager entityManager;

    // Cache for prepared query strings
    private final Map<String, String> queryCache = new ConcurrentHashMap<>();

    /**
     * Execute optimized range query for BigDecimal values
     * @param tableName the table name
     * @param columnName the column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return list of matching records
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> executeRangeQuery(String tableName, String columnName, 
                                           BigDecimal minValue, BigDecimal maxValue) {
        String cacheKey = String.format("range_%s_%s", tableName, columnName);
        String queryString = queryCache.computeIfAbsent(cacheKey, k -> 
            String.format("SELECT * FROM %s WHERE %s BETWEEN :minValue AND :maxValue", tableName, columnName)
        );

        Query query = entityManager.createNativeQuery(queryString);
        query.setParameter("minValue", minValue);
        query.setParameter("maxValue", maxValue);

        return query.getResultList();
    }

    /**
     * Execute optimized aggregation query for BigDecimal columns
     * @param tableName the table name
     * @param columnName the column name
     * @param aggregationType the aggregation type (SUM, AVG, MIN, MAX)
     * @return aggregation result
     */
    public BigDecimal executeAggregationQuery(String tableName, String columnName, String aggregationType) {
        String cacheKey = String.format("agg_%s_%s_%s", aggregationType, tableName, columnName);
        String queryString = queryCache.computeIfAbsent(cacheKey, k -> 
            String.format("SELECT %s(%s) FROM %s", aggregationType, columnName, tableName)
        );

        Query query = entityManager.createNativeQuery(queryString);
        Object result = query.getSingleResult();
        
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }

    /**
     * Get database performance statistics for BigDecimal columns
     * @return performance statistics
     */
    public DatabasePerformanceStats getDatabasePerformanceStats() {
        try {
            // Query to get table sizes and index usage for DCF-related tables
            String tableStatsQuery = """
                SELECT 
                    schemaname,
                    tablename,
                    attname as column_name,
                    n_distinct,
                    correlation
                FROM pg_stats 
                WHERE tablename IN ('dcf_inputs', 'dcf_outputs', 'financial_data')
                AND attname LIKE '%rate%' OR attname LIKE '%value%' OR attname LIKE '%price%'
                ORDER BY tablename, attname
                """;

            Query query = entityManager.createNativeQuery(tableStatsQuery);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            return new DatabasePerformanceStats(results);

        } catch (Exception e) {
            logger.error("Error retrieving database performance statistics", e);
            return new DatabasePerformanceStats();
        }
    }

    /**
     * Generate index recommendations for BigDecimal columns
     * @return list of recommended indexes
     */
    public List<String> generateIndexRecommendations() {
        return List.of(
            // Indexes for DCF input queries
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_inputs_discount_rate ON dcf_inputs(discount_rate) WHERE discount_rate IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_inputs_growth_rate ON dcf_inputs(growth_rate) WHERE growth_rate IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_inputs_terminal_growth_rate ON dcf_inputs(terminal_growth_rate) WHERE terminal_growth_rate IS NOT NULL;",
            
            // Indexes for DCF output queries
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_outputs_fair_value ON dcf_outputs(fair_value_per_share) WHERE fair_value_per_share IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_outputs_current_price ON dcf_outputs(current_price) WHERE current_price IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_outputs_enterprise_value ON dcf_outputs(enterprise_value) WHERE enterprise_value IS NOT NULL;",
            
            // Composite indexes for common query patterns
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_inputs_rates_composite ON dcf_inputs(discount_rate, growth_rate, terminal_growth_rate);",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dcf_outputs_valuation_composite ON dcf_outputs(fair_value_per_share, current_price, calculated_at);",
            
            // Indexes for financial data
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_financial_data_fcf ON financial_data(latest_free_cash_flow) WHERE latest_free_cash_flow IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_financial_data_debt ON financial_data(latest_debt) WHERE latest_debt IS NOT NULL;",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_financial_data_shares ON financial_data(latest_shares_outstanding) WHERE latest_shares_outstanding IS NOT NULL;"
        );
    }

    /**
     * Analyze query performance for BigDecimal operations
     * @param queryString the query to analyze
     * @return query execution plan
     */
    public String analyzeQueryPerformance(String queryString) {
        try {
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + queryString;
            Query query = entityManager.createNativeQuery(explainQuery);
            
            @SuppressWarnings("unchecked")
            List<Object> results = query.getResultList();
            
            return results.toString();
            
        } catch (Exception e) {
            logger.error("Error analyzing query performance", e);
            return "Error analyzing query: " + e.getMessage();
        }
    }

    /**
     * Optimize BigDecimal column storage
     * @return list of optimization recommendations
     */
    public List<String> getStorageOptimizationRecommendations() {
        return List.of(
            // Column type optimizations
            "Consider using NUMERIC(precision, scale) instead of DECIMAL for better performance",
            "Set appropriate precision and scale based on data requirements:",
            "  - Rates: NUMERIC(10,6) for percentage rates with 6 decimal precision",
            "  - Prices: NUMERIC(20,6) for stock prices with 6 decimal precision", 
            "  - Large values: NUMERIC(25,2) for enterprise values with 2 decimal precision",
            
            // Storage optimizations
            "Use NOT NULL constraints where appropriate to improve query performance",
            "Consider partitioning large tables by date ranges for historical data",
            "Implement table compression for historical DCF calculation data",
            
            // Query optimizations
            "Use prepared statements for repeated BigDecimal queries",
            "Batch INSERT/UPDATE operations for better performance",
            "Consider using connection pooling with appropriate pool sizes"
        );
    }

    /**
     * Database performance statistics
     */
    public static class DatabasePerformanceStats {
        private final List<Object[]> columnStats;
        private final int totalColumns;

        public DatabasePerformanceStats() {
            this.columnStats = List.of();
            this.totalColumns = 0;
        }

        public DatabasePerformanceStats(List<Object[]> columnStats) {
            this.columnStats = columnStats;
            this.totalColumns = columnStats.size();
        }

        public List<Object[]> getColumnStats() { return columnStats; }
        public int getTotalColumns() { return totalColumns; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Database Performance Statistics:\n");
            sb.append("Total BigDecimal columns analyzed: ").append(totalColumns).append("\n");
            
            for (Object[] row : columnStats) {
                sb.append(String.format("Table: %s, Column: %s, Distinct values: %s, Correlation: %s\n",
                    row[1], row[2], row[3], row[4]));
            }
            
            return sb.toString();
        }
    }
}