# Implementation Plan

## Backend Entity Layer Tasks

- [x] 1. Update DCFInput entity to use BigDecimal
  - Convert discountRate, growthRate, terminalGrowthRate from Double to BigDecimal
  - Update JPA column annotations with appropriate precision and scale
  - Update utility methods (getDiscountRateAsDecimal, etc.) for BigDecimal
  - _Requirements: 1.1, 4.1_

- [x] 2. Update DCFOutput entity to use BigDecimal
  - Convert all Double fields to BigDecimal (fairValuePerShare, currentPrice, etc.)
  - Add appropriate JPA column annotations with precision and scale
  - Update constructors and setter methods for BigDecimal
  - _Requirements: 1.1, 4.1_

## Backend DTO Layer Tasks

- [x] 3. Update DCFInputDto to use BigDecimal
  - Convert Double fields to BigDecimal
  - Update validation annotations (@DecimalMin, @DecimalMax, @Digits)
  - Update conversion methods between DTO and entity
  - _Requirements: 1.2, 3.3_

- [x] 4. Update DCFOutputDto to use BigDecimal
  - Convert all Double fields to BigDecimal
  - Update JSON serialization annotations
  - Update conversion methods from entity to DTO
  - _Requirements: 1.2, 3.1, 3.2_

## Backend Service Layer Tasks

- [x] 5. Update DCFCalculationService arithmetic operations
  - Convert all mathematical operations to use BigDecimal arithmetic
  - Implement proper rounding modes (HALF_UP) for all division operations
  - Update projectFreeCashFlows method to use BigDecimal
  - Update calculatePresentValue method to use BigDecimal
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 6. Update DCFCalculationService calculation methods
  - Convert performDCFCalculation method to use BigDecimal throughout
  - Update terminal value calculations with BigDecimal precision
  - Update sensitivity analysis methods for BigDecimal
  - Add proper error handling for BigDecimal arithmetic exceptions
  - _Requirements: 2.1, 2.3, 2.4_

- [x] 7. Update validation utilities for BigDecimal
  - Update DCFValidationUtil to handle BigDecimal validation
  - Add BigDecimal range validation methods
  - Update precision and scale validation logic
  - Add meaningful error messages for BigDecimal validation failures
  - _Requirements: 1.3, 3.4_

## Backend API Layer Tasks

- [x] 8. Update FinancialDataController for BigDecimal handling
  - Update request parameter parsing to handle BigDecimal strings
  - Update response formatting for BigDecimal serialization
  - Add error handling for BigDecimal parsing exceptions
  - _Requirements: 3.1, 3.3, 3.4_

- [x] 9. Configure Jackson for BigDecimal serialization
  - Configure ObjectMapper to serialize BigDecimal as plain strings
  - Add custom serializers if needed to prevent scientific notation
  - Update JSON configuration to maintain decimal precision
  - Test serialization with very large and small BigDecimal values
  - _Requirements: 1.4, 3.1, 3.2_

## Database Layer Tasks

- [ ] 10. Create database migration for BigDecimal columns
  - Create migration script to add new DECIMAL columns alongside existing DOUBLE columns
  - Add data conversion logic to populate BigDecimal columns from Double values
  - Create validation queries to ensure data integrity during migration
  - _Requirements: 4.1, 4.2, 4.3_

- [ ] 11. Complete database migration and cleanup
  - Execute migration in staging environment and validate results
  - Drop old Double columns after successful validation
  - Rename new DECIMAL columns to original names
  - Update database indexes for new DECIMAL columns
  - _Requirements: 4.1, 4.2, 4.3_

## Testing Tasks

- [x] 12. Update unit tests for BigDecimal arithmetic
  - Modify existing DCFCalculationService tests to use BigDecimal assertions
  - Update test data to use BigDecimal values instead of Double values
  - Add tests for BigDecimal rounding behavior and precision handling
  - Create tests for edge cases with very large and very small BigDecimal values
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 13. Create integration tests for BigDecimal API handling
  - Test API serialization and deserialization of BigDecimal values
  - Verify JSON responses contain plain decimal strings without scientific notation
  - Test API validation with BigDecimal input parameters
  - Create end-to-end tests for complete DCF calculation accuracy
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 14. Create database integration tests for BigDecimal persistence
  - Test BigDecimal storage and retrieval from database
  - Verify precision and scale are maintained during database operations
  - Test database queries with BigDecimal parameters
  - Validate migration scripts with test data
  - _Requirements: 4.1, 4.2, 4.3_

## Frontend Integration Tasks

- [-] 15. Update frontend services for BigDecimal handling
  - Update financialService.ts to handle BigDecimal string values from APIs
  - Add proper parsing and formatting for BigDecimal values
  - Update error handling for BigDecimal parsing failures
  - _Requirements: 5.1, 5.3_

- [x] 15.1 Fix Watchlist component BigDecimal type handling
  - Update formatPrice function in Watchlist.tsx to handle string | undefined types
  - Add proper BigDecimal string parsing for fair_value_per_share and current_price
  - Ensure proper error handling for invalid BigDecimal string values
  - Test with various BigDecimal string formats from API
  - _Requirements: 5.1, 5.4_

- [x] 16. Update DCF Calculator component for BigDecimal display
  - Update DCFCalculator.tsx to handle BigDecimal string inputs and outputs
  - Implement proper formatting for BigDecimal display (avoid scientific notation)
  - Update input validation to work with BigDecimal precision
  - Add user-friendly formatting for large monetary values
  - _Requirements: 5.1, 5.2, 5.4_

- [x] 17. Update charts and visualization for BigDecimal values
  - Update chart components to handle BigDecimal string values
  - Implement proper formatting for chart axes and tooltips
  - Ensure BigDecimal values display correctly in all visualization components
  - Test with very large and small BigDecimal values
  - _Requirements: 5.2, 5.4_

## Performance and Optimization Tasks

- [x] 18. Optimize BigDecimal performance
  - Profile application performance with BigDecimal operations
  - Implement caching strategies for BigDecimal calculations
  - Optimize database queries with DECIMAL columns
  - Add performance monitoring for BigDecimal-heavy operations
  - _Requirements: 4.4_

- [x] 19. Final validation and deployment preparation
  - Conduct comprehensive testing across all application layers
  - Validate BigDecimal precision in production-like environment
  - Create deployment checklist for BigDecimal migration
  - Prepare rollback procedures in case of issues
  - _Requirements: 1.1, 2.4, 3.2, 4.2, 5.4_