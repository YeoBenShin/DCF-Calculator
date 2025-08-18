# Design Document

## Overview

This design document outlines the conversion of the DCF Calculator application from using Double precision floating-point numbers to BigDecimal for all financial calculations and data storage. The conversion addresses precision issues that occur with floating-point arithmetic, particularly when dealing with large monetary values that may be displayed in scientific notation.

The current codebase has a mixed implementation where FinancialData entities already use BigDecimal, but DCF calculation services, input/output entities, and DTOs still use Double. This creates type mismatches and precision loss during calculations.

## Architecture

The BigDecimal conversion affects multiple layers of the application:

**Data Layer (Entities)**
- DCFInput entity: Convert Double fields to BigDecimal
- DCFOutput entity: Convert Double fields to BigDecimal
- FinancialData entity: Already uses BigDecimal (no changes needed)

**Service Layer**
- DCFCalculationService: Convert all mathematical operations to BigDecimal arithmetic
- FinancialDataScrapingService: Ensure BigDecimal parsing from scraped data
- Validation utilities: Update to handle BigDecimal validation

**API Layer (DTOs and Controllers)**
- DCFInputDto: Convert Double fields to BigDecimal
- DCFOutputDto: Convert Double fields to BigDecimal
- FinancialDataDto: Already uses BigDecimal (no changes needed)
- Controllers: Update request/response handling for BigDecimal

**Database Layer**
- Update column types from DOUBLE to DECIMAL with appropriate precision and scale
- Create migration scripts for existing data conversion

## Components and Interfaces

### Entity Layer Changes

#### DCFInput Entity
```java
// Convert from Double to BigDecimal
private BigDecimal discountRate;        // Was: Double discountRate
private BigDecimal growthRate;          // Was: Double growthRate  
private BigDecimal terminalGrowthRate;  // Was: Double terminalGrowthRate

// Update utility methods
public BigDecimal getDiscountRateAsDecimal() {
    return discountRate != null ? discountRate.divide(new BigDecimal("100")) : null;
}
```

#### DCFOutput Entity
```java
// Convert all Double fields to BigDecimal
private BigDecimal fairValuePerShare;
private BigDecimal currentPrice;
private BigDecimal upsideDownsidePercentage;
private BigDecimal terminalValue;
private BigDecimal presentValueOfCashFlows;
private BigDecimal enterpriseValue;
private BigDecimal equityValue;
private BigDecimal sharesOutstanding;
```

### DTO Layer Changes

#### DCFInputDto
```java
// Convert validation annotations and field types
@DecimalMin(value = "0.0", message = "Discount rate must be positive")
@DecimalMax(value = "100.0", message = "Discount rate must be less than 100%")
private BigDecimal discountRate;
```

#### DCFOutputDto
```java
// Convert all Double fields to BigDecimal
private BigDecimal fairValuePerShare;
private BigDecimal currentPrice;
```

### Service Layer Changes

#### DCFCalculationService
The most significant changes occur in the calculation service:

```java
// BigDecimal arithmetic operations
private List<BigDecimal> projectFreeCashFlows(BigDecimal baseFCF, BigDecimal growthRate, int years) {
    List<BigDecimal> projectedFCFs = new ArrayList<>();
    BigDecimal growthMultiplier = BigDecimal.ONE.add(growthRate);
    
    for (int i = 1; i <= years; i++) {
        BigDecimal projectedFCF = baseFCF.multiply(growthMultiplier.pow(i));
        projectedFCFs.add(projectedFCF);
    }
    return projectedFCFs;
}

// Present value calculations with BigDecimal
private BigDecimal calculatePresentValue(List<BigDecimal> cashFlows, BigDecimal discountRate) {
    BigDecimal presentValue = BigDecimal.ZERO;
    BigDecimal discountMultiplier = BigDecimal.ONE.add(discountRate);
    
    for (int i = 0; i < cashFlows.size(); i++) {
        BigDecimal cashFlow = cashFlows.get(i);
        int year = i + 1;
        BigDecimal pv = cashFlow.divide(discountMultiplier.pow(year), 10, RoundingMode.HALF_UP);
        presentValue = presentValue.add(pv);
    }
    return presentValue;
}
```

## Data Models

### Updated DCFInput Model
```java
@Entity
public class DCFInput {
    @Column(precision = 10, scale = 6)
    private BigDecimal discountRate;
    
    @Column(precision = 10, scale = 6) 
    private BigDecimal growthRate;
    
    @Column(precision = 10, scale = 6)
    private BigDecimal terminalGrowthRate;
}
```

### Updated DCFOutput Model
```java
@Entity
public class DCFOutput {
    @Column(precision = 20, scale = 6)
    private BigDecimal fairValuePerShare;
    
    @Column(precision = 20, scale = 6)
    private BigDecimal currentPrice;
    
    @Column(precision = 25, scale = 2)
    private BigDecimal enterpriseValue;
    
    @Column(precision = 25, scale = 2)
    private BigDecimal equityValue;
}
```

## BigDecimal Configuration Standards

### Precision and Scale Guidelines
- **Percentage rates** (discount, growth): precision=10, scale=6 (allows 9999.999999%)
- **Share prices**: precision=20, scale=6 (allows prices up to $99,999,999,999,999.999999)
- **Large monetary values** (enterprise value): precision=25, scale=2 (allows trillions with cent precision)
- **Share counts**: precision=20, scale=0 (whole shares only)

### Rounding Mode Standards
- **Financial calculations**: RoundingMode.HALF_UP (standard financial rounding)
- **Display formatting**: RoundingMode.HALF_UP with 2-6 decimal places
- **Intermediate calculations**: RoundingMode.HALF_UP with 10 decimal places for accuracy

### Mathematical Operations
```java
// Division operations must specify scale and rounding mode
BigDecimal result = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

// Multiplication and addition preserve precision automatically
BigDecimal product = value1.multiply(value2);
BigDecimal sum = value1.add(value2);

// Power operations for compound calculations
BigDecimal compounded = base.pow(exponent); // For integer exponents only
```

## JSON Serialization Configuration

### Jackson Configuration
```java
@JsonSerialize(using = ToStringSerializer.class)
private BigDecimal fairValuePerShare;

// Or globally configure ObjectMapper
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    return mapper;
}
```

### Frontend Integration
- API responses will contain BigDecimal values as plain decimal strings
- Frontend parsing: `parseFloat(value)` or `Number(value)` for display
- Input validation: Convert string inputs to BigDecimal on backend

## Database Migration Strategy

### Migration Steps
1. **Add new BigDecimal columns** alongside existing Double columns
2. **Data conversion script** to populate BigDecimal columns from Double values
3. **Application deployment** with dual-column support
4. **Validation period** to ensure data integrity
5. **Drop old Double columns** after successful validation

### Sample Migration SQL
```sql
-- Add new BigDecimal columns
ALTER TABLE dcf_inputs 
ADD COLUMN discount_rate_decimal DECIMAL(10,6),
ADD COLUMN growth_rate_decimal DECIMAL(10,6),
ADD COLUMN terminal_growth_rate_decimal DECIMAL(10,6);

-- Convert existing data
UPDATE dcf_inputs 
SET discount_rate_decimal = discount_rate,
    growth_rate_decimal = growth_rate,
    terminal_growth_rate_decimal = terminal_growth_rate;

-- Drop old columns (after validation)
ALTER TABLE dcf_inputs 
DROP COLUMN discount_rate,
DROP COLUMN growth_rate, 
DROP COLUMN terminal_growth_rate;

-- Rename new columns
ALTER TABLE dcf_inputs 
RENAME COLUMN discount_rate_decimal TO discount_rate,
RENAME COLUMN growth_rate_decimal TO growth_rate,
RENAME COLUMN terminal_growth_rate_decimal TO terminal_growth_rate;
```

## Error Handling

### BigDecimal-Specific Error Handling
```java
try {
    BigDecimal result = numerator.divide(denominator, 10, RoundingMode.HALF_UP);
} catch (ArithmeticException e) {
    throw new DCFCalculationException("Division by zero in DCF calculation", e);
}

// Validation for reasonable BigDecimal ranges
if (growthRate.compareTo(new BigDecimal("10.0")) > 0) {
    throw new ValidationException("Growth rate exceeds reasonable bounds");
}
```

### Input Validation Updates
```java
@DecimalMin(value = "0.0", message = "Discount rate must be positive")
@DecimalMax(value = "100.0", message = "Discount rate must be less than 100%")
@Digits(integer = 4, fraction = 6, message = "Invalid discount rate format")
private BigDecimal discountRate;
```

## Testing Strategy

### Unit Testing Approach
- **BigDecimal equality testing**: Use `compareTo()` instead of `equals()`
- **Precision testing**: Verify calculations maintain expected decimal places
- **Rounding testing**: Ensure consistent rounding behavior across operations
- **Boundary testing**: Test with very large and very small values

### Integration Testing
- **API serialization**: Verify JSON output contains plain decimal strings
- **Database persistence**: Confirm BigDecimal values store and retrieve correctly
- **End-to-end calculations**: Validate complete DCF calculation accuracy

### Performance Considerations
- **BigDecimal operations** are slower than primitive double operations
- **Memory usage** is higher for BigDecimal objects
- **Caching strategy** may need adjustment for BigDecimal-based calculations
- **Database indexing** on DECIMAL columns may perform differently than DOUBLE