# BigDecimal Migration Deployment Checklist

## Pre-Deployment Validation

### 1. Code Review and Testing
- [ ] All unit tests pass with BigDecimal implementation
- [ ] Integration tests validate BigDecimal precision across all layers
- [ ] Performance tests confirm acceptable BigDecimal operation speed
- [ ] API serialization tests verify no scientific notation in responses
- [ ] Database migration scripts tested in staging environment

### 2. Database Preparation
- [ ] Database backup completed and verified
- [ ] Migration scripts reviewed and approved
- [ ] Rollback scripts prepared and tested
- [ ] Database connection pool settings reviewed for DECIMAL column performance
- [ ] Index performance validated for new DECIMAL columns

### 3. Environment Validation
- [ ] Staging environment matches production configuration
- [ ] All BigDecimal validation tests pass in staging
- [ ] Load testing completed with BigDecimal operations
- [ ] Memory usage profiled with BigDecimal objects
- [ ] JVM heap size adequate for BigDecimal memory overhead

## Deployment Steps

### Phase 1: Database Migration
- [ ] **Step 1.1**: Deploy database migration to add new DECIMAL columns
  - Execute: `V1__add_bigdecimal_columns.sql`
  - Verify: New columns exist with correct precision/scale
  - Rollback: `R1__remove_bigdecimal_columns.sql` if needed

- [ ] **Step 1.2**: Run data conversion script
  - Execute: `V2__convert_double_to_decimal.sql`
  - Verify: All data converted successfully
  - Check: No precision loss during conversion
  - Rollback: Restore from backup if conversion fails

- [ ] **Step 1.3**: Validate data integrity
  - Run: `BigDecimalMigrationValidationSuite.validateDataMigrationIntegrity()`
  - Check: All BigDecimal values within expected ranges
  - Verify: No null values where not expected

### Phase 2: Application Deployment
- [ ] **Step 2.1**: Deploy application with BigDecimal support
  - Deploy: Backend application with BigDecimal entities/services
  - Verify: Application starts successfully
  - Check: Health check endpoints respond correctly

- [ ] **Step 2.2**: Validate API responses
  - Test: All financial data endpoints return BigDecimal as plain strings
  - Verify: No scientific notation in JSON responses
  - Check: API validation works with BigDecimal inputs

- [ ] **Step 2.3**: Frontend deployment
  - Deploy: Frontend with BigDecimal string handling
  - Verify: Financial values display correctly without scientific notation
  - Test: User input validation works with BigDecimal precision

### Phase 3: Production Validation
- [ ] **Step 3.1**: Smoke tests
  - Test: DCF calculation with known inputs produces expected results
  - Verify: Database operations complete successfully
  - Check: API responses maintain precision

- [ ] **Step 3.2**: Performance monitoring
  - Monitor: Response times for BigDecimal operations
  - Check: Memory usage within acceptable limits
  - Verify: Database query performance acceptable

- [ ] **Step 3.3**: User acceptance testing
  - Test: End-to-end DCF calculations with real user scenarios
  - Verify: Large monetary values display correctly
  - Check: Precision maintained through complete user workflows

## Post-Deployment Cleanup

### Phase 4: Column Cleanup (After 24-48 hours of stable operation)
- [ ] **Step 4.1**: Final validation before cleanup
  - Run: Complete validation suite one more time
  - Verify: All BigDecimal operations working correctly
  - Check: No issues reported by users or monitoring

- [ ] **Step 4.2**: Remove old Double columns
  - Execute: `V3__drop_double_columns.sql`
  - Verify: Old columns removed successfully
  - Update: Database documentation

- [ ] **Step 4.3**: Rename DECIMAL columns to original names
  - Execute: `V4__rename_decimal_columns.sql`
  - Verify: Column names match original schema
  - Update: Application configuration if needed

## Monitoring and Alerts

### Key Metrics to Monitor
- [ ] API response times for financial calculations
- [ ] Database query performance for DECIMAL columns
- [ ] Memory usage and garbage collection frequency
- [ ] Error rates for BigDecimal parsing/validation
- [ ] User-reported issues with financial value display

### Alert Thresholds
- [ ] API response time > 2x baseline
- [ ] Database query time > 1.5x baseline
- [ ] Memory usage > 85% of allocated heap
- [ ] Error rate > 1% for BigDecimal operations
- [ ] Any scientific notation detected in API responses

## Success Criteria

### Technical Success
- [ ] All financial calculations maintain exact precision
- [ ] No scientific notation in user-facing displays
- [ ] Performance degradation < 20% from Double implementation
- [ ] Zero data loss during migration
- [ ] All automated tests passing

### Business Success
- [ ] Users can perform DCF calculations without precision issues
- [ ] Large monetary values display correctly
- [ ] Historical calculation results remain accessible
- [ ] No user-reported calculation accuracy issues

## Risk Mitigation

### High-Risk Areas
- [ ] **Database Migration**: Risk of data loss or corruption
  - Mitigation: Full backup, tested rollback procedures
- [ ] **Performance Impact**: BigDecimal operations slower than Double
  - Mitigation: Performance testing, caching strategies
- [ ] **Memory Usage**: BigDecimal objects use more memory
  - Mitigation: JVM tuning, memory monitoring

### Contingency Plans
- [ ] **Immediate Rollback**: If critical issues discovered within 2 hours
- [ ] **Partial Rollback**: If specific components fail
- [ ] **Data Recovery**: If database corruption occurs
- [ ] **Performance Rollback**: If unacceptable performance degradation

## Communication Plan

### Stakeholder Notifications
- [ ] **Pre-deployment**: Notify users of maintenance window
- [ ] **During deployment**: Status updates every 30 minutes
- [ ] **Post-deployment**: Confirmation of successful migration
- [ ] **Issues**: Immediate notification if problems arise

### Documentation Updates
- [ ] Update API documentation with BigDecimal format specifications
- [ ] Update database schema documentation
- [ ] Update developer guides with BigDecimal best practices
- [ ] Update troubleshooting guides with BigDecimal-specific issues