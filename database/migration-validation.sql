-- BigDecimal Migration Database Validation Script
-- This script validates the database state before, during, and after BigDecimal migration

-- =====================================================
-- PRE-MIGRATION VALIDATION
-- =====================================================

-- Check current table structure
SELECT 'PRE-MIGRATION TABLE STRUCTURE' as validation_step;

DESCRIBE dcf_inputs;
DESCRIBE dcf_outputs;
DESCRIBE financial_data;

-- Count existing records
SELECT 'PRE-MIGRATION RECORD COUNTS' as validation_step;

SELECT 
    'dcf_inputs' as table_name,
    COUNT(*) as record_count,
    COUNT(CASE WHEN discount_rate IS NOT NULL THEN 1 END) as discount_rate_count,
    COUNT(CASE WHEN growth_rate IS NOT NULL THEN 1 END) as growth_rate_count,
    COUNT(CASE WHEN terminal_growth_rate IS NOT NULL THEN 1 END) as terminal_growth_rate_count
FROM dcf_inputs;

SELECT 
    'dcf_outputs' as table_name,
    COUNT(*) as record_count,
    COUNT(CASE WHEN fair_value_per_share IS NOT NULL THEN 1 END) as fair_value_count,
    COUNT(CASE WHEN current_price IS NOT NULL THEN 1 END) as current_price_count,
    COUNT(CASE WHEN enterprise_value IS NOT NULL THEN 1 END) as enterprise_value_count
FROM dcf_outputs;

SELECT 
    'financial_data' as table_name,
    COUNT(*) as record_count,
    COUNT(CASE WHEN revenue IS NOT NULL THEN 1 END) as revenue_count,
    COUNT(CASE WHEN free_cash_flow IS NOT NULL THEN 1 END) as fcf_count
FROM financial_data;

-- Check for data quality issues
SELECT 'PRE-MIGRATION DATA QUALITY CHECKS' as validation_step;

-- Check for extreme values that might cause issues
SELECT 
    'dcf_inputs_extreme_values' as check_name,
    COUNT(*) as count
FROM dcf_inputs 
WHERE discount_rate > 100 OR discount_rate < 0 
   OR growth_rate > 100 OR growth_rate < -50
   OR terminal_growth_rate > 50 OR terminal_growth_rate < -10;

SELECT 
    'dcf_outputs_extreme_values' as check_name,
    COUNT(*) as count
FROM dcf_outputs 
WHERE fair_value_per_share > 10000 OR fair_value_per_share < 0
   OR enterprise_value > 1000000000000 OR enterprise_value < 0;

-- =====================================================
-- MIGRATION PROGRESS VALIDATION
-- =====================================================

-- Check if BigDecimal columns have been added
SELECT 'MIGRATION PROGRESS - COLUMN EXISTENCE' as validation_step;

SELECT 
    table_name,
    column_name,
    data_type,
    numeric_precision,
    numeric_scale
FROM information_schema.columns 
WHERE table_schema = DATABASE()
  AND table_name IN ('dcf_inputs', 'dcf_outputs', 'financial_data')
  AND data_type IN ('double', 'decimal')
ORDER BY table_name, column_name;

-- Check data conversion progress (if dual columns exist)
SELECT 'MIGRATION PROGRESS - DATA CONVERSION' as validation_step;

-- For dcf_inputs table
SELECT 
    'dcf_inputs_conversion_status' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN discount_rate IS NOT NULL AND discount_rate_decimal IS NOT NULL THEN 1 END) as both_populated,
    COUNT(CASE WHEN discount_rate IS NOT NULL AND discount_rate_decimal IS NULL THEN 1 END) as only_double,
    COUNT(CASE WHEN discount_rate IS NULL AND discount_rate_decimal IS NOT NULL THEN 1 END) as only_decimal
FROM dcf_inputs
WHERE EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
      AND table_name = 'dcf_inputs' 
      AND column_name = 'discount_rate_decimal'
);

-- =====================================================
-- POST-MIGRATION VALIDATION
-- =====================================================

-- Verify BigDecimal precision is maintained
SELECT 'POST-MIGRATION PRECISION VALIDATION' as validation_step;

-- Check precision and scale of decimal columns
SELECT 
    table_name,
    column_name,
    data_type,
    numeric_precision,
    numeric_scale,
    CASE 
        WHEN data_type = 'decimal' AND numeric_precision >= 10 THEN 'OK'
        ELSE 'INSUFFICIENT_PRECISION'
    END as precision_status
FROM information_schema.columns 
WHERE table_schema = DATABASE()
  AND table_name IN ('dcf_inputs', 'dcf_outputs', 'financial_data')
  AND data_type = 'decimal';

-- Validate data integrity after migration
SELECT 'POST-MIGRATION DATA INTEGRITY' as validation_step;

-- Check for null values in critical fields
SELECT 
    'dcf_inputs_null_check' as table_name,
    COUNT(CASE WHEN discount_rate IS NULL THEN 1 END) as null_discount_rate,
    COUNT(CASE WHEN growth_rate IS NULL THEN 1 END) as null_growth_rate,
    COUNT(CASE WHEN terminal_growth_rate IS NULL THEN 1 END) as null_terminal_growth_rate
FROM dcf_inputs;

SELECT 
    'dcf_outputs_null_check' as table_name,
    COUNT(CASE WHEN fair_value_per_share IS NULL THEN 1 END) as null_fair_value,
    COUNT(CASE WHEN enterprise_value IS NULL THEN 1 END) as null_enterprise_value,
    COUNT(CASE WHEN equity_value IS NULL THEN 1 END) as null_equity_value
FROM dcf_outputs;

-- Check for reasonable value ranges
SELECT 'POST-MIGRATION VALUE RANGE VALIDATION' as validation_step;

SELECT 
    'dcf_inputs_range_check' as check_name,
    MIN(discount_rate) as min_discount_rate,
    MAX(discount_rate) as max_discount_rate,
    MIN(growth_rate) as min_growth_rate,
    MAX(growth_rate) as max_growth_rate,
    MIN(terminal_growth_rate) as min_terminal_growth_rate,
    MAX(terminal_growth_rate) as max_terminal_growth_rate
FROM dcf_inputs;

SELECT 
    'dcf_outputs_range_check' as check_name,
    MIN(fair_value_per_share) as min_fair_value,
    MAX(fair_value_per_share) as max_fair_value,
    MIN(enterprise_value) as min_enterprise_value,
    MAX(enterprise_value) as max_enterprise_value
FROM dcf_outputs;

-- =====================================================
-- PERFORMANCE VALIDATION
-- =====================================================

-- Test query performance with BigDecimal columns
SELECT 'PERFORMANCE VALIDATION' as validation_step;

-- Create test queries to measure performance
SET @start_time = NOW(6);

SELECT COUNT(*) 
FROM dcf_inputs d
JOIN dcf_outputs o ON d.id = o.dcf_input_id
WHERE d.discount_rate > 5.0 
  AND o.fair_value_per_share > 10.0;

SET @end_time = NOW(6);

SELECT 
    'query_performance_test' as test_name,
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000 as execution_time_ms;

-- =====================================================
-- ROLLBACK VALIDATION
-- =====================================================

-- Verify rollback capability (if backup tables exist)
SELECT 'ROLLBACK VALIDATION' as validation_step;

-- Check if backup tables exist
SELECT 
    table_name,
    table_rows
FROM information_schema.tables 
WHERE table_schema = DATABASE()
  AND table_name LIKE '%_backup%'
  OR table_name LIKE '%_pre_migration%';

-- =====================================================
-- FINAL VALIDATION SUMMARY
-- =====================================================

SELECT 'FINAL VALIDATION SUMMARY' as validation_step;

-- Overall health check
SELECT 
    'database_health' as check_category,
    CASE 
        WHEN (SELECT COUNT(*) FROM dcf_inputs) > 0 
         AND (SELECT COUNT(*) FROM dcf_outputs) > 0 
         AND (SELECT COUNT(*) FROM financial_data) > 0 
        THEN 'HEALTHY'
        ELSE 'ISSUES_DETECTED'
    END as status;

-- BigDecimal implementation status
SELECT 
    'bigdecimal_implementation' as check_category,
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.columns 
              WHERE table_schema = DATABASE() 
                AND table_name IN ('dcf_inputs', 'dcf_outputs') 
                AND data_type = 'decimal') > 0 
        THEN 'IMPLEMENTED'
        ELSE 'NOT_IMPLEMENTED'
    END as status;

-- Data integrity status
SELECT 
    'data_integrity' as check_category,
    CASE 
        WHEN (SELECT COUNT(*) FROM dcf_inputs WHERE discount_rate < 0 OR discount_rate > 100) = 0
         AND (SELECT COUNT(*) FROM dcf_outputs WHERE fair_value_per_share < 0) = 0
        THEN 'VALID'
        ELSE 'ISSUES_DETECTED'
    END as status;

-- Generate validation report timestamp
SELECT 
    'validation_completed' as status,
    NOW() as timestamp,
    DATABASE() as database_name,
    USER() as validated_by;