# Implementation Plan

- [x] 1. Set up project structure and core interfaces
  - Create directory structure for frontend (`frontend/`) and backend (`backend/`)
  - Define TypeScript interfaces and Java DTOs for API communication
  - Set up build configuration files (package.json, pom.xml)
  - _Requirements: General Setup for Requirements 2, 3, 4_

- [ ] 2. Implement data models and validation
  - [x] 2.1 Create User data model with validation
    - Write User class/interface with fields: user_id, email, password_hash, watchlist
    - Implement email validation and password hashing utilities
    - Write unit tests for User model validation
    - _Requirements: 1.3_

  - [x] 2.2 Create FinancialData model with validation
    - Write FinancialData class/interface with all financial metrics fields
    - Implement data type validation for numerical fields
    - Write unit tests for FinancialData model validation
    - _Requirements: 2.1_

  - [x] 2.3 Create DCF input and output models with validation
    - Write DCFInput class/interface with discount_rate, growth_rate, terminal_growth_rate
    - Write DCFOutput class/interface with fair_value_per_share, current_price, valuation
    - Implement growth rate validation (reject >1000%)
    - Write unit tests for DCF model validation
    - _Requirements: 2.4, 5.3_

- [ ] 3. Create core backend services
  - [x] 3.1 Implement web scraping service
    - Write service class to accept ticker symbol and return FinancialData
    - Implement web scraping logic for financial websites
    - Add error handling for invalid tickers and network timeouts
    - Write unit tests for scraping service with mock data
    - _Requirements: 2.1, 2.2, 5.1_

  - [x] 3.2 Implement DCF calculation service
    - Write service class to accept DCFInput and FinancialData, return DCFOutput
    - Implement DCF mathematical formula and calculations
    - Add logic to determine valuation status (undervalued/overvalued)
    - Write unit tests for DCF calculations with known inputs/outputs
    - _Requirements: 3.1, 3.2_

  - [x] 3.3 Implement authentication service
    - Write service class for user registration and login
    - Implement JWT token generation and validation
    - Add password hashing and verification
    - Write unit tests for authentication flows
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 3.4 Implement watchlist service
    - Write service class to manage user watchlists
    - Implement add/remove ticker functionality
    - Add watchlist retrieval with latest fair value status
    - Write unit tests for watchlist operations
    - _Requirements: 4.1, 4.2, 4.3_

- [ ] 4. Build REST API endpoints
  - [x] 4.1 Create authentication endpoints
    - Implement POST /auth/signup endpoint with input validation
    - Implement POST /auth/login endpoint with credential verification
    - Add middleware for JWT token validation
    - Write integration tests for auth endpoints
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 4.2 Create financial data endpoints
    - Implement GET /financials?ticker={symbol} endpoint
    - Add input validation and error handling for invalid tickers
    - Implement caching mechanism for financial data
    - Write integration tests for financial endpoints
    - _Requirements: 2.1, 2.2, 5.1_

  - [x] 4.3 Create DCF calculation endpoints
    - Implement POST /calculateDCF endpoint accepting DCFInput
    - Add validation for required fields and realistic growth rates
    - Return DCFOutput with fair value and valuation status
    - Write integration tests for DCF calculation endpoint
    - _Requirements: 3.1, 3.2, 5.2, 5.3_

  - [x] 4.4 Create watchlist endpoints
    - Implement GET /watchlist endpoint for authenticated users
    - Implement POST /watchlist/add and DELETE /watchlist/remove endpoints
    - Add authorization checks for user-specific watchlists
    - Write integration tests for watchlist endpoints
    - _Requirements: 4.1, 4.2, 4.3_

- [-] 5. Develop frontend components
  - [x] 5.1 Create authentication pages
    - Build Login component with email/password form
    - Build Signup component with validation
    - Implement API integration for auth endpoints
    - Add form validation and error display
    - Write component tests for authentication pages
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 5.2 Create DCF input and search page
    - Build stock ticker search component
    - Build DCF parameters input form (discount rate, growth rates)
    - Add form validation for required fields and realistic values
    - Implement API integration for financial data fetching
    - Write component tests for input validation
    - _Requirements: 2.1, 2.3, 2.4, 5.2, 5.3_

  - [x] 5.3 Create results display page
    - Build fair value results card component
    - Build financial charts component using Chart.js or Recharts
    - Display revenue, net income, free cash flow, and EPS charts
    - Add loading states and error handling
    - Write component tests for results display
    - _Requirements: 3.2, 3.3_

  - [x] 5.4 Create watchlist page
    - Build watchlist table/list component
    - Implement add/remove stock functionality
    - Display latest fair value status for each stock
    - Add loading states and error handling
    - Write component tests for watchlist functionality
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 6. Integration and end-to-end testing
  - [x] 6.1 Wire frontend and backend together
    - Configure API base URLs and endpoints
    - Implement global error handling and loading states
    - Add authentication token management
    - Test complete user flows manually
    - _Requirements: All requirements_

  - [x] 6.2 Write end-to-end automated tests
    - Set up Cypress or Playwright testing framework
    - Write tests for complete user registration and login flow
    - Write tests for stock search, DCF calculation, and results display
    - Write tests for watchlist add/remove functionality
    - Write tests for error scenarios and edge cases
    - _Requirements: 5.1, 5.2, 5.3_