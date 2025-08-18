# BigDecimal Migration Rollback Procedures

## Overview

This document outlines the rollback procedures for the BigDecimal migration in case issues are discovered during or after deployment. The procedures are designed to restore the system to its previous Double-based implementation with minimal data loss.

## Rollback Decision Matrix

### Immediate Rollback Triggers (0-2 hours)
- **Critical System Failure**: Application won't start or crashes repeatedly
- **Data Corruption**: Evidence of data loss or corruption during migration
- **Complete API Failure**: All financial endpoints returning errors
- **Database Corruption**: Database integrity checks fail

### Planned Rollback Triggers (2-24 hours)
- **Performance Degradation**: >50% increase in response times
- **Memory Issues**: Out of memory errors or excessive garbage collection
- **User-Reported Issues**: Multiple reports of calculation inaccuracies
- **Scientific Notation**: BigDecimal values displaying in scientific notation

### Extended Rollback Triggers (24+ hours)
- **Business Impact**: Significant user complaints or business disruption
- **Stability Issues**: Intermittent failures affecting user experience
- **Integration Problems**: Third-party integrations failing due to format changes

## Rollback Procedures

### Phase 1: Immediate Application Rollback (Emergency - 15 minutes)

#### Step 1.1: Stop Current Application
```bash
# Stop the application immediately
sudo systemctl stop dcf-calculator-backend
sudo systemctl stop dcf-calculator-frontend

# Verify services are stopped
sudo systemctl status dcf-calculator-backend
sudo systemctl status dcf-calculator-frontend
```

#### Step 1.2: Deploy Previous Version
```bash
# Switch to previous application version
cd /opt/dcf-calculator
git checkout tags/v1.0.0-pre-bigdecimal  # Replace with actual tag

# Rebuild and deploy previous version
mvn clean package -DskipTests
sudo cp target/dcf-calculator-*.jar /opt/dcf-calculator/
sudo systemctl start dcf-calculator-backend

# Deploy previous frontend version
cd /opt/dcf-calculator-frontend
npm run build:production
sudo systemctl start dcf-calculator-frontend
```

#### Step 1.3: Verify Application Recovery
```bash
# Check application health
curl http://localhost:8080/actuator/health
curl http://localhost:3000/health

# Test basic functionality
curl http://localhost:8080/api/financial-data/AAPL
```

### Phase 2: Database Rollback (30-60 minutes)

#### Step 2.1: Assess Database State
```sql
-- Check if BigDecimal columns exist
DESCRIBE dcf_inputs;
DESCRIBE dcf_outputs;
DESCRIBE financial_data;

-- Verify data integrity
SELECT COUNT(*) FROM dcf_inputs WHERE discount_rate IS NULL;
SELECT COUNT(*) FROM dcf_outputs WHERE fair_value_per_share IS NULL;
```

#### Step 2.2: Restore from Backup (If Migration Completed)
```bash
# Stop application to prevent data changes
sudo systemctl stop dcf-calculator-backend

# Restore database from pre-migration backup
mysql -u root -p dcf_calculator < /backups/dcf_calculator_pre_bigdecimal_$(date +%Y%m%d).sql

# Verify backup restoration
mysql -u root -p -e "USE dcf_calculator; SHOW TABLES; DESCRIBE dcf_inputs;"
```

#### Step 2.3: Partial Rollback (If Migration In Progress)
```sql
-- If migration is partially complete, revert to Double columns
-- Step 1: Copy data back to Double columns (if they still exist)
UPDATE dcf_inputs 
SET discount_rate = discount_rate_decimal,
    growth_rate = growth_rate_decimal,
    terminal_growth_rate = terminal_growth_rate_decimal
WHERE discount_rate_decimal IS NOT NULL;

-- Step 2: Drop BigDecimal columns
ALTER TABLE dcf_inputs 
DROP COLUMN discount_rate_decimal,
DROP COLUMN growth_rate_decimal,
DROP COLUMN terminal_growth_rate_decimal;

-- Step 3: Repeat for other tables
UPDATE dcf_outputs 
SET fair_value_per_share = fair_value_per_share_decimal,
    current_price = current_price_decimal,
    enterprise_value = enterprise_value_decimal,
    equity_value = equity_value_decimal
WHERE fair_value_per_share_decimal IS NOT NULL;

ALTER TABLE dcf_outputs 
DROP COLUMN fair_value_per_share_decimal,
DROP COLUMN current_price_decimal,
DROP COLUMN enterprise_value_decimal,
DROP COLUMN equity_value_decimal;
```

### Phase 3: Configuration Rollback

#### Step 3.1: Revert Application Configuration
```bash
# Restore previous application.yml
cd /opt/dcf-calculator/src/main/resources
git checkout HEAD~1 application.yml

# Restore previous Jackson configuration
cd /opt/dcf-calculator/src/main/java/com/dcf/config
git checkout HEAD~1 JacksonConfig.java
```

#### Step 3.2: Revert Database Configuration
```bash
# Update database connection settings if needed
# Restore previous Hibernate dialect settings
# Revert any BigDecimal-specific database optimizations
```

### Phase 4: Validation and Testing

#### Step 4.1: Functional Testing
```bash
# Run critical path tests
cd /opt/dcf-calculator
mvn test -Dtest=DCFCalculationServiceTest
mvn test -Dtest=FinancialDataControllerTest

# Test API endpoints
curl -X POST http://localhost:8080/api/dcf/calculate \
  -H "Content-Type: application/json" \
  -d '{"ticker":"AAPL","discountRate":8.5,"growthRate":3.2}'
```

#### Step 4.2: Data Integrity Verification
```sql
-- Verify all financial data is accessible
SELECT COUNT(*) FROM dcf_inputs;
SELECT COUNT(*) FROM dcf_outputs;
SELECT COUNT(*) FROM financial_data;

-- Check for any data anomalies
SELECT * FROM dcf_inputs WHERE discount_rate < 0 OR discount_rate > 100;
SELECT * FROM dcf_outputs WHERE fair_value_per_share < 0;
```

#### Step 4.3: Performance Validation
```bash
# Run performance tests to ensure rollback didn't degrade performance
cd /opt/dcf-calculator
mvn test -Dtest=DCFCalculationPerformanceTest

# Monitor system resources
top -p $(pgrep java)
free -h
```

## Rollback Validation Checklist

### Application Layer
- [ ] Application starts successfully
- [ ] All endpoints respond correctly
- [ ] No errors in application logs
- [ ] Health checks pass
- [ ] Authentication/authorization working

### Database Layer
- [ ] All tables accessible
- [ ] Data counts match pre-migration
- [ ] No null values in critical fields
- [ ] Query performance acceptable
- [ ] Database integrity checks pass

### API Layer
- [ ] All endpoints return expected data types (Double)
- [ ] JSON serialization working correctly
- [ ] Input validation functioning
- [ ] Error handling appropriate
- [ ] Response times acceptable

### Frontend Layer
- [ ] Application loads correctly
- [ ] Financial data displays properly
- [ ] User interactions work
- [ ] No JavaScript errors
- [ ] Charts and visualizations render

## Data Recovery Procedures

### Scenario 1: Partial Data Loss
```sql
-- If some data was lost during migration, recover from backup
-- Create temporary table with backup data
CREATE TABLE dcf_inputs_backup AS 
SELECT * FROM dcf_inputs_pre_migration;

-- Identify missing records
SELECT b.* FROM dcf_inputs_backup b
LEFT JOIN dcf_inputs c ON b.id = c.id
WHERE c.id IS NULL;

-- Restore missing records
INSERT INTO dcf_inputs 
SELECT * FROM dcf_inputs_backup 
WHERE id NOT IN (SELECT id FROM dcf_inputs);
```

### Scenario 2: Data Corruption
```bash
# Full database restore from backup
mysql -u root -p dcf_calculator < /backups/dcf_calculator_pre_bigdecimal.sql

# Verify restoration
mysql -u root -p -e "
USE dcf_calculator;
SELECT COUNT(*) as input_count FROM dcf_inputs;
SELECT COUNT(*) as output_count FROM dcf_outputs;
SELECT COUNT(*) as financial_count FROM financial_data;
"
```

## Communication During Rollback

### Internal Team Notifications
```bash
# Send immediate notification to development team
echo "URGENT: BigDecimal migration rollback in progress. System may be unavailable." | \
  mail -s "DCF Calculator Rollback Alert" dev-team@company.com

# Update status page
curl -X POST https://status.company.com/api/incidents \
  -H "Authorization: Bearer $STATUS_API_KEY" \
  -d '{"status":"investigating","message":"Rolling back BigDecimal migration due to issues"}'
```

### User Communications
- **Immediate**: Display maintenance message on frontend
- **15 minutes**: Email notification to active users
- **30 minutes**: Status update on company blog/status page
- **Resolution**: Confirmation that service is restored

## Post-Rollback Analysis

### Required Documentation
- [ ] Root cause analysis of rollback trigger
- [ ] Timeline of rollback execution
- [ ] Data integrity verification results
- [ ] Performance impact assessment
- [ ] Lessons learned and improvements needed

### Follow-up Actions
- [ ] Review and improve migration procedures
- [ ] Update rollback procedures based on experience
- [ ] Plan for re-attempting BigDecimal migration
- [ ] Implement additional safeguards
- [ ] Update monitoring and alerting

## Prevention Measures for Future Migrations

### Enhanced Testing
- More comprehensive staging environment testing
- Extended performance testing with production-like data
- User acceptance testing with real scenarios
- Automated rollback testing

### Improved Monitoring
- Real-time data integrity monitoring
- Performance baseline comparisons
- User experience monitoring
- Automated rollback triggers

### Better Preparation
- More granular rollback procedures
- Automated rollback scripts
- Enhanced backup strategies
- Improved communication templates