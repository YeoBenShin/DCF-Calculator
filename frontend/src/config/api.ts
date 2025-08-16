// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  TIMEOUT: 30000, // 30 seconds
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000, // 1 second
};

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    SIGNUP: '/auth/signup',
  },
  FINANCIAL: {
    GET_DATA: '/financials',
    CALCULATE_DCF: '/dcf/calculate',
  },
  WATCHLIST: {
    GET: '/watchlist',
    ADD: '/watchlist/add',
    REMOVE: '/watchlist/remove',
  },
};

// Error Messages
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  SERVER_ERROR: 'Server error. Please try again later.',
  UNAUTHORIZED: 'Your session has expired. Please log in again.',
  TICKER_NOT_FOUND: 'Ticker not found.',
  SCRAPING_FAILED: 'Unable to retrieve financials at the moment.',
  INVALID_CREDENTIALS: 'Invalid email or password',
  EMAIL_EXISTS: 'Email already exists. Please use a different email.',
  VALIDATION_ERROR: 'Please check your input and try again.',
};

// App Configuration
export const APP_CONFIG = {
  TOKEN_STORAGE_KEY: 'authToken',
  USER_STORAGE_KEY: 'user',
  ERROR_DISPLAY_DURATION: 5000, // 5 seconds
  LOADING_DEBOUNCE: 300, // 300ms
};