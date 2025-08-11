# DCF Calculator - End-to-End Tests

This directory contains comprehensive end-to-end tests for the DCF Calculator application using Cypress.

## Test Structure

### Test Files

- **`auth.cy.js`** - Authentication flow tests (login, signup, logout)
- **`dcf-calculation.cy.js`** - DCF calculation and stock search functionality tests
- **`watchlist.cy.js`** - Watchlist management tests (add, remove, display)
- **`error-scenarios.cy.js`** - Error handling and edge case tests
- **`all-tests.cy.js`** - Complete user journey and integration tests

### Support Files

- **`support/commands.js`** - Custom Cypress commands for common operations
- **`support/e2e.js`** - Global configuration and setup

## Running Tests

### Prerequisites

1. Ensure both frontend and backend servers are running:
   ```bash
   # Terminal 1 - Backend (Spring Boot)
   cd backend
   ./mvnw spring-boot:run
   
   # Terminal 2 - Frontend (React)
   cd frontend
   npm start
   ```

2. Backend should be running on `http://localhost:8080`
3. Frontend should be running on `http://localhost:3000`

### Running Tests

#### Interactive Mode (Cypress Test Runner)
```bash
cd frontend
npm run cypress:open
# or
npm run test:e2e:open
```

#### Headless Mode (CI/CD)
```bash
cd frontend
npm run cypress:run
# or
npm run test:e2e
```

#### Run Specific Test File
```bash
cd frontend
npx cypress run --spec "cypress/e2e/auth.cy.js"
```

#### Run Tests with Specific Browser
```bash
cd frontend
npx cypress run --browser chrome
npx cypress run --browser firefox
npx cypress run --browser edge
```

## Test Coverage

### Authentication Tests
- ✅ User registration with validation
- ✅ User login with credential verification
- ✅ Logout functionality
- ✅ Error handling for invalid credentials
- ✅ Form validation for empty fields

### DCF Calculation Tests
- ✅ Stock search with valid ticker symbols
- ✅ Financial data retrieval and display
- ✅ DCF parameter input validation
- ✅ Fair value calculation and results display
- ✅ Financial charts rendering
- ✅ Error handling for invalid tickers
- ✅ Validation for unrealistic growth rates

### Watchlist Tests
- ✅ Add stocks to watchlist
- ✅ Remove stocks from watchlist
- ✅ Display watchlist with fair value status
- ✅ Empty watchlist state
- ✅ Watchlist navigation
- ✅ Authentication requirements for watchlist access

### Error Scenarios
- ✅ Network failures and timeouts
- ✅ API server errors (4xx, 5xx)
- ✅ Malformed API responses
- ✅ Authentication token expiration
- ✅ Input validation edge cases
- ✅ Data integrity issues
- ✅ UI state management during errors

### Integration Tests
- ✅ Complete user journey from registration to watchlist management
- ✅ Error recovery scenarios
- ✅ Performance under rapid interactions
- ✅ Browser navigation (back/forward)
- ✅ Page refresh handling

## Test Data and Mocking

All tests use mocked API responses to ensure:
- **Reliability**: Tests don't depend on external services
- **Speed**: Fast execution without network delays
- **Consistency**: Predictable test data
- **Isolation**: Tests don't affect real data

### Mock Data Examples

```javascript
// Financial Data Mock
const mockFinancialData = {
  ticker: 'AAPL',
  revenue: ['365.8B', '274.5B', '260.2B', '229.2B'],
  free_cash_flow: ['111.5B', '73.4B', '70.1B', '56.1B'],
  // ... other financial metrics
}

// DCF Result Mock
const mockDCFResult = {
  ticker: 'AAPL',
  fair_value_per_share: 173.45,
  current_price: 150.00,
  valuation: 'Undervalued'
}
```

## Custom Commands

The test suite includes custom Cypress commands for common operations:

```javascript
// Login with default or custom credentials
cy.login('user@example.com', 'password123')

// Register new user
cy.register('newuser@example.com', 'password123')

// Clear authentication state
cy.clearAuth()

// Mock API responses
cy.mockApiResponse('GET', '**/financials*', mockData)

// Wait for specific API calls
cy.waitForApi('@GET**/financials*')
```

## Test Data Attributes

All interactive elements in the application should have `data-testid` attributes for reliable test selection:

```html
<!-- Good -->
<input data-testid="ticker-input" />
<button data-testid="search-button">Search</button>

<!-- Avoid -->
<input className="ticker-input" />
<button id="search-btn">Search</button>
```

## Debugging Tests

### Screenshots and Videos
- Screenshots are automatically taken on test failures
- Videos are disabled by default for faster execution
- Enable videos in `cypress.config.js` if needed

### Debug Mode
```bash
# Run with debug output
DEBUG=cypress:* npm run cypress:run

# Run single test with browser open
npx cypress run --spec "cypress/e2e/auth.cy.js" --headed --no-exit
```

### Browser Developer Tools
When running in interactive mode, you can:
1. Open browser developer tools
2. Inspect elements and network requests
3. Set breakpoints in test code
4. Use `cy.debug()` and `cy.pause()` commands

## Continuous Integration

For CI/CD pipelines, use the headless mode:

```yaml
# Example GitHub Actions
- name: Run E2E Tests
  run: |
    cd frontend
    npm run test:e2e
  env:
    CYPRESS_baseUrl: http://localhost:3000
    CYPRESS_apiUrl: http://localhost:8080
```

## Best Practices

1. **Test Independence**: Each test should be able to run independently
2. **Clean State**: Use `beforeEach` to reset state between tests
3. **Descriptive Names**: Use clear, descriptive test and describe block names
4. **Mock External Dependencies**: Always mock API calls and external services
5. **Wait for Elements**: Use `cy.wait()` for API calls and `cy.should()` for element assertions
6. **Data Attributes**: Use `data-testid` attributes instead of CSS classes or IDs
7. **Error Scenarios**: Test both happy path and error scenarios
8. **Real User Interactions**: Simulate actual user behavior and workflows

## Troubleshooting

### Common Issues

1. **Tests timing out**: Increase timeout values in `cypress.config.js`
2. **Elements not found**: Ensure `data-testid` attributes are present
3. **API mocks not working**: Check intercept patterns and timing
4. **Flaky tests**: Add proper waits and assertions

### Getting Help

- Check Cypress documentation: https://docs.cypress.io/
- Review test logs and screenshots in `cypress/screenshots/`
- Use `cy.debug()` to pause test execution
- Enable verbose logging with `DEBUG=cypress:*`