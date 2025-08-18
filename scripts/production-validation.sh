#!/bin/bash

# BigDecimal Migration Production Validation Script
# This script validates the BigDecimal migration in a production-like environment

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DB_HOST="${DB_HOST:-localhost}"
DB_NAME="${DB_NAME:-dcf_calculator}"
DB_USER="${DB_USER:-dcf_user}"
LOG_FILE="validation_$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Success/failure tracking
TESTS_PASSED=0
TESTS_FAILED=0
CRITICAL_FAILURES=()

# Test result function
test_result() {
    if [ $1 -eq 0 ]; then
        log "${GREEN}✓ $2${NC}"
        ((TESTS_PASSED++))
    else
        log "${RED}✗ $2${NC}"
        ((TESTS_FAILED++))
        if [ "$3" = "critical" ]; then
            CRITICAL_FAILURES+=("$2")
        fi
    fi
}

log "${YELLOW}Starting BigDecimal Migration Production Validation${NC}"
log "API Base URL: $API_BASE_URL"
log "Database: $DB_HOST/$DB_NAME"

# Test 1: Application Health Check
log "\n=== Test 1: Application Health Check ==="
if curl -s -f "$API_BASE_URL/actuator/health" > /dev/null; then
    test_result 0 "Application health check" "critical"
else
    test_result 1 "Application health check" "critical"
fi

# Test 2: Database Connectivity
log "\n=== Test 2: Database Connectivity ==="
if mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" -e "USE $DB_NAME; SELECT 1;" > /dev/null 2>&1; then
    test_result 0 "Database connectivity" "critical"
else
    test_result 1 "Database connectivity" "critical"
fi

# Test 3: BigDecimal Column Validation
log "\n=== Test 3: BigDecimal Column Validation ==="
DECIMAL_COLUMNS=$(mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" -e "
USE $DB_NAME;
SELECT COUNT(*) FROM information_schema.columns 
WHERE table_schema = '$DB_NAME' 
AND data_type = 'decimal' 
AND table_name IN ('dcf_inputs', 'dcf_outputs', 'financial_data');
" -s -N)

if [ "$DECIMAL_COLUMNS" -gt 0 ]; then
    test_result 0 "BigDecimal columns exist ($DECIMAL_COLUMNS found)" "critical"
else
    test_result 1 "BigDecimal columns exist" "critical"
fi

# Test 4: Data Integrity Check
log "\n=== Test 4: Data Integrity Check ==="
NULL_COUNT=$(mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" -e "
USE $DB_NAME;
SELECT 
    (SELECT COUNT(*) FROM dcf_inputs WHERE discount_rate IS NULL AND discount_rate IS NOT NULL) +
    (SELECT COUNT(*) FROM dcf_outputs WHERE fair_value_per_share IS NULL AND fair_value_per_share IS NOT NULL) as null_count;
" -s -N)

if [ "$NULL_COUNT" -eq 0 ]; then
    test_result 0 "Data integrity check (no unexpected nulls)"
else
    test_result 1 "Data integrity check ($NULL_COUNT unexpected nulls found)" "critical"
fi

# Test 5: API Response Format Validation
log "\n=== Test 5: API Response Format Validation ==="
API_RESPONSE=$(curl -s "$API_BASE_URL/api/financial-data/AAPL" || echo "ERROR")

if [ "$API_RESPONSE" != "ERROR" ]; then
    # Check for scientific notation
    if echo "$API_RESPONSE" | grep -qE '[0-9]+\.?[0-9]*[eE][+-]?[0-9]+'; then
        test_result 1 "API response format (contains scientific notation)" "critical"
    else
        test_result 0 "API response format (no scientific notation)"
    fi
    
    # Check for decimal values
    if echo "$API_RESPONSE" | grep -qE '"[0-9]+\.[0-9]+"'; then
        test_result 0 "API response contains decimal values"
    else
        test_result 1 "API response format (no decimal values found)"
    fi
else
    test_result 1 "API response retrieval" "critical"
fi

# Test 6: DCF Calculation Precision Test
log "\n=== Test 6: DCF Calculation Precision Test ==="
DCF_REQUEST='{
    "ticker": "TEST",
    "discountRate": 8.123456,
    "growthRate": 3.987654,
    "terminalGrowthRate": 2.555555,
    "freeCashFlow": 1000000000,
    "revenue": 5000000000,
    "sharesOutstanding": 1000000000
}'

DCF_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/dcf/calculate" \
    -H "Content-Type: application/json" \
    -d "$DCF_REQUEST" || echo "ERROR")

if [ "$DCF_RESPONSE" != "ERROR" ]; then
    # Check if response contains fairValuePerShare
    if echo "$DCF_RESPONSE" | grep -q "fairValuePerShare"; then
        test_result 0 "DCF calculation execution"
        
        # Extract fair value and check precision
        FAIR_VALUE=$(echo "$DCF_RESPONSE" | grep -o '"fairValuePerShare":"[^"]*"' | cut -d'"' -f4)
        if [[ "$FAIR_VALUE" =~ ^[0-9]+\.[0-9]{2,}$ ]]; then
            test_result 0 "DCF calculation precision (fair value: $FAIR_VALUE)"
        else
            test_result 1 "DCF calculation precision (unexpected format: $FAIR_VALUE)"
        fi
    else
        test_result 1 "DCF calculation execution (no fair value in response)" "critical"
    fi
else
    test_result 1 "DCF calculation execution" "critical"
fi

# Test 7: Large Value Handling
log "\n=== Test 7: Large Value Handling ==="
LARGE_VALUE_REQUEST='{
    "ticker": "LARGE",
    "discountRate": 8.5,
    "growthRate": 3.2,
    "terminalGrowthRate": 2.5,
    "freeCashFlow": 999999999999,
    "revenue": 5000000000000,
    "sharesOutstanding": 1000000000
}'

LARGE_RESPONSE=$(curl -s -X POST "$API_BASE_URL/api/dcf/calculate" \
    -H "Content-Type: application/json" \
    -d "$LARGE_VALUE_REQUEST" || echo "ERROR")

if [ "$LARGE_RESPONSE" != "ERROR" ]; then
    if echo "$LARGE_RESPONSE" | grep -qE '[0-9]+\.?[0-9]*[eE][+-]?[0-9]+'; then
        test_result 1 "Large value handling (scientific notation detected)" "critical"
    else
        test_result 0 "Large value handling (no scientific notation)"
    fi
else
    test_result 1 "Large value calculation" "critical"
fi

# Test 8: Performance Baseline
log "\n=== Test 8: Performance Baseline ==="
START_TIME=$(date +%s%N)
for i in {1..10}; do
    curl -s "$API_BASE_URL/api/dcf/calculate" \
        -H "Content-Type: application/json" \
        -d "$DCF_REQUEST" > /dev/null
done
END_TIME=$(date +%s%N)

DURATION_MS=$(( (END_TIME - START_TIME) / 1000000 ))
AVG_RESPONSE_TIME=$(( DURATION_MS / 10 ))

log "Average response time: ${AVG_RESPONSE_TIME}ms"

if [ "$AVG_RESPONSE_TIME" -lt 2000 ]; then
    test_result 0 "Performance baseline (avg: ${AVG_RESPONSE_TIME}ms)"
else
    test_result 1 "Performance baseline (avg: ${AVG_RESPONSE_TIME}ms - exceeds 2000ms threshold)"
fi

# Test 9: Memory Usage Check
log "\n=== Test 9: Memory Usage Check ==="
JAVA_PID=$(pgrep -f "dcf-calculator" | head -1)
if [ -n "$JAVA_PID" ]; then
    MEMORY_MB=$(ps -p "$JAVA_PID" -o rss= | awk '{print int($1/1024)}')
    log "Application memory usage: ${MEMORY_MB}MB"
    
    if [ "$MEMORY_MB" -lt 2048 ]; then
        test_result 0 "Memory usage check (${MEMORY_MB}MB)"
    else
        test_result 1 "Memory usage check (${MEMORY_MB}MB - exceeds 2GB threshold)"
    fi
else
    test_result 1 "Memory usage check (application process not found)"
fi

# Test 10: Database Query Performance
log "\n=== Test 10: Database Query Performance ==="
QUERY_START=$(date +%s%N)
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" -e "
USE $DB_NAME;
SELECT d.*, f.* FROM dcf_inputs d 
JOIN financial_data f ON d.ticker = f.ticker 
WHERE d.discount_rate > 5.0 
LIMIT 100;
" > /dev/null 2>&1
QUERY_END=$(date +%s%N)

QUERY_TIME_MS=$(( (QUERY_END - QUERY_START) / 1000000 ))
log "Database query time: ${QUERY_TIME_MS}ms"

if [ "$QUERY_TIME_MS" -lt 1000 ]; then
    test_result 0 "Database query performance (${QUERY_TIME_MS}ms)"
else
    test_result 1 "Database query performance (${QUERY_TIME_MS}ms - exceeds 1000ms threshold)"
fi

# Summary
log "\n${YELLOW}=== Validation Summary ===${NC}"
log "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
log "Tests Failed: ${RED}$TESTS_FAILED${NC}"

if [ ${#CRITICAL_FAILURES[@]} -gt 0 ]; then
    log "\n${RED}CRITICAL FAILURES:${NC}"
    for failure in "${CRITICAL_FAILURES[@]}"; do
        log "${RED}  - $failure${NC}"
    done
    log "\n${RED}RECOMMENDATION: DO NOT PROCEED WITH DEPLOYMENT${NC}"
    exit 1
else
    if [ $TESTS_FAILED -eq 0 ]; then
        log "\n${GREEN}ALL TESTS PASSED - READY FOR DEPLOYMENT${NC}"
        exit 0
    else
        log "\n${YELLOW}SOME NON-CRITICAL TESTS FAILED - REVIEW BEFORE DEPLOYMENT${NC}"
        exit 2
    fi
fi