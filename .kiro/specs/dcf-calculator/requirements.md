# Requirements Document

## Introduction

This finance web application allows users to compute the intrinsic value of stocks using the Discounted Cash Flow (DCF) model. Users input a stock ticker and the application displays relevant historical financial information through visualization techniques such as charts. Using this information, users can then decide on relevant financial parameters (discount rate, growth rate, terminal growth rate). The backend retrieves financial data via web scraping, performs DCF calculations using data from both web scraping and user input, and displays fair value alongside historical financial charts. Users can also save assets to a personal watchlist. The app supports authentication for personalized experiences.

## Requirements

### Requirement 1: User Authentication

**User Story:** As a new or returning user, I want to sign up or log in securely, so that I can access my personalized data and watchlist.

#### Acceptance Criteria

1. WHEN a user submits valid login credentials THEN the system SHALL authenticate the user and issue a session or JWT token
2. IF the user submits invalid credentials THEN the system SHALL return an error message and prevent access
3. WHEN a new user signs up with a unique email THEN the system SHALL hash the password and store user credentials securely

### Requirement 2: DCF Input and Financial Fetching

**User Story:** As an investor, I want to input a stock ticker and my own DCF parameters, so that I can get a customized fair value estimate.

#### Acceptance Criteria

1. WHEN a user submits a valid stock ticker THEN the system SHALL fetch relevant financials via web scraping
2. IF the stock ticker is invalid THEN the system SHALL return an error message: "Ticker not found."
3. IF any required DCF parameters are missing THEN the system SHALL prevent form submission and highlight missing fields
4. IF the user inputs a growth rate > 1000% THEN the system SHALL alert the user to input a realistic value

### Requirement 3: DCF Output and Result Display

**User Story:** As a user, I want to see the computed fair value of a stock and whether it is undervalued or overvalued, so that I can make informed investment decisions.

#### Acceptance Criteria

1. WHEN valid financial data and user DCF inputs are submitted THEN the system SHALL compute the fair value using the DCF model
2. WHEN the calculation completes THEN the system SHALL display the fair value per share, current price, and valuation status (e.g., "Undervalued")
3. WHEN results are displayed THEN the system SHALL also show charts for revenue, net income, free cash flow, and EPS

### Requirement 4: Watchlist Functionality

**User Story:** As a logged-in user, I want to save stocks to a watchlist, so that I can track their valuations over time.

#### Acceptance Criteria

1. WHEN a user adds a stock to their watchlist THEN the system SHALL store the ticker under the user's profile
2. WHEN a user visits the Watchlist Page THEN the system SHALL retrieve and display saved stocks and their latest fair value status
3. WHEN a user removes a stock from the watchlist THEN the system SHALL update the user profile and reflect the change on the UI

### Requirement 5: Error Handling

**User Story:** As a user, I want helpful messages when errors occur, so that I know what went wrong and how to fix it.

#### Acceptance Criteria

1. IF web scraping fails due to timeout or connection issues THEN the system SHALL notify the user: "Unable to retrieve financials at the moment."
2. IF the input form is submitted with missing required fields THEN the system SHALL highlight those fields in red and block submission
3. WHEN invalid data is input (e.g., excessively high growth rate) THEN the system SHALL return an informative warning or prompt for correction