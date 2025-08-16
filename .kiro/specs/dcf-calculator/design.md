# Design Document

## Overview

This web application is a finance tool designed to help users calculate the fair value of an asset using the Discounted Cash Flow (DCF) model. The user will input a stock ticker symbol, and the backend will scrape financial data from the web (e.g., income statement, cash flow statement, balance sheet).

Users will provide additional DCF parameters such as the discount rate, predicted annual growth rate, and terminal growth rate. Using both the scraped data and user inputs, the application will compute the intrinsic value of the asset.

The application will also visualize financial data through charts and graphs to help users understand a company's financial trends over time.

## Architecture

The architecture follows a client-server model with the following key components:

**Frontend (React)**
- Landing Page
- Login / Signup Page
- Asset Searching Page
- Results Display Page (Charts) + DCF Calculator Input Fields
- Results Display Page (Fair value + Charts)
- Watchlist Page

**Backend (Spring Boot)**
- Web scraping module to extract financials using stock ticker symbol
- DCF calculation logic module
- API endpoints for data retrieval and calculations
- Authentication module (JWT-based or session-based)
- Optional: cache financials for speed

**Database (Firebase)**
- User data (email, password hash, watchlist)
- Cached financial data for performance
- History of user calculations (optional)

## Components and Interfaces

### Login / Signup Page
- **UI:** Email, password inputs
- **Backend:** POST /auth/signup, /auth/login
- **Logic:** JWT or session token generation, password hashing

### DCF Calculator Input Page
- **UI:** Ticker symbol, discount rate, predicted growth rate, terminal growth rate inputs
- **Backend:**
  - GET /financials?ticker=AAPL – fetch scraped financials
  - POST /calculateDCF – submit financials and inputs to calculate DCF

### Results Page
- **UI:** Display fair value output and whether it's over- or under-valued
- **Charts:** Revenue, net income, free cash flow, EPS (using Chart.js or Recharts)
- **Backend:** Receives and returns calculated results via API

### Watchlist Page
- **UI:** Shows saved stocks and fair value status
- **Backend:** GET /watchlist, POST /watchlist/add, DELETE /watchlist/remove

## Data Models

### User
```json
{
  "userId": "uuid",
  "email": "string",
  "password_hash": "string",
  "watchlist": ["AAPL", "GOOGL"]
}
```

### Financial Data
```json
{
  "ticker": "AAPL",
  "revenue": ["TTM: 120B", "2020: 100B", "2021: 120B"],
  "operating_expense": [...],
  "operating_income": [...],
  "operating_cash_flow": [...],
  "net_profit": [...],
  "capital_expenditure": [...],
  "free_cash_flow": [...],
  "eps": [...],
  "total_debt": [...],
  "ordinary_shares_number": [...],
  "date_fetched": "2025-08-01"
}
```

### DCF Input
```json
{
  "ticker": "AAPL",
  "discountRate": 8.5,
  "growthRate": 12,
  "terminalGrowthRate": 2.5
}
```

### DCF Output
```json
{
  "ticker": "AAPL",
  "fair_value_per_share": 173.45,
  "current_price": 150.00,
  "valuation": "Undervalued"
}
```

## Error Handling

- **Invalid Ticker Symbol** → Return an error message: "Ticker not found." and redirect to input page
- **Excessive Growth Rate (>1000%)** → Show alert: "Growth rate too high. Please input a realistic value."
- **Web Scraping Timeout or Failure** → Inform the user: "Unable to retrieve financials at the moment." and allow retry
- **Missing Required Fields** → Prevent submission and highlight required fields in red

## Testing Strategy

### Unit Testing
Test each backend module individually: scraping, DCF calculation, input validation

### Integration Testing
Simulate full user flow from input to result display using tools like Postman / Jest

### Frontend Testing
Use React Testing Library or Cypress to verify UI renders and input validation

### Error Handling Testing
Simulate bad inputs, network errors, and invalid tickers to test robustness