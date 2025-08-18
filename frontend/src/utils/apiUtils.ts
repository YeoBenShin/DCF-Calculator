import { AxiosError, AxiosResponse } from 'axios';
import { API_CONFIG, ERROR_MESSAGES } from '../config/api';

// Utility function to delay execution
export const delay = (ms: number): Promise<void> => 
  new Promise(resolve => setTimeout(resolve, ms));

// Utility function to retry API calls
export const retryApiCall = async <T>(
  apiCall: () => Promise<AxiosResponse<T>>,
  maxRetries: number = API_CONFIG.RETRY_ATTEMPTS,
  retryDelay: number = API_CONFIG.RETRY_DELAY
): Promise<AxiosResponse<T>> => {
  let lastError: AxiosError;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await apiCall();
    } catch (error) {
      lastError = error as AxiosError;
      
      // Don't retry on client errors (4xx) except 408 (timeout)
      if (lastError.response?.status && 
          lastError.response.status >= 400 && 
          lastError.response.status < 500 && 
          lastError.response.status !== 408) {
        throw lastError;
      }
      
      // Don't retry on the last attempt
      if (attempt === maxRetries) {
        throw lastError;
      }
      
      // Wait before retrying
      await delay(retryDelay * attempt); // Exponential backoff
    }
  }
  
  throw lastError!;
};

// Utility function to extract error message from API response
export const extractErrorMessage = (error: any): string => {
  // Check for direct error message from backend
  if (error.response?.data?.error) {
    return error.response.data.error;
  }
  
  // Check for message field from backend
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  
  if (error.response?.status) {
    switch (error.response.status) {
      case 400:
        return error.response.data?.message || ERROR_MESSAGES.VALIDATION_ERROR;
      case 401:
        return ERROR_MESSAGES.UNAUTHORIZED;
      case 404:
        return ERROR_MESSAGES.TICKER_NOT_FOUND;
      case 409:
        return ERROR_MESSAGES.EMAIL_EXISTS;
      case 500:
      case 502:
      case 503:
      case 504:
        return ERROR_MESSAGES.SERVER_ERROR;
      default:
        return error.response.data?.message || ERROR_MESSAGES.SERVER_ERROR;
    }
  }
  
  if (error.code === 'NETWORK_ERROR' || !error.response) {
    return ERROR_MESSAGES.NETWORK_ERROR;
  }
  
  return error.message || 'An unexpected error occurred';
};

// Utility function to check if error is retryable
export const isRetryableError = (error: AxiosError): boolean => {
  // Network errors are retryable
  if (!error.response) {
    return true;
  }
  
  const status = error.response.status;
  
  // Server errors (5xx) and timeout (408) are retryable
  return status >= 500 || status === 408;
};

// Utility function to format API response
export const formatApiResponse = <T>(response: AxiosResponse<any>): T => {
  // Check for error responses first
  if (response.data.error) {
    throw new Error(response.data.error);
  }
  
  // Handle wrapped response with success and data fields
  if (response.data.success && response.data.data !== undefined) {
    return response.data.data;
  }
  
  // Handle wrapped response with message and data fields
  if (response.data.data !== undefined) {
    return response.data.data;
  }
  
  // If no data field, return the response directly (most common case now)
  return response.data;
};

// Utility function to validate ticker symbol
export const validateTicker = (ticker: string): boolean => {
  // Basic ticker validation: 1-5 uppercase letters
  const tickerRegex = /^[A-Z]{1,5}$/;
  return tickerRegex.test(ticker.toUpperCase());
};

// Utility function to format currency (supports both number and BigDecimal string)
export const formatCurrency = (amount: number | string): string => {
  const numericAmount = typeof amount === 'string' ? parseBigDecimal(amount) : amount;
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericAmount);
};

// Utility function to format percentage (supports both number and BigDecimal string)
export const formatPercentage = (value: number | string): string => {
  const numericValue = typeof value === 'string' ? parseBigDecimal(value) : value;
  return new Intl.NumberFormat('en-US', {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericValue / 100);
};

// Utility function to format large numbers with appropriate suffixes (K, M, B, T)
export const formatLargeNumber = (value: number | string): string => {
  const numericValue = typeof value === 'string' ? parseBigDecimal(value) : value;
  
  if (Math.abs(numericValue) >= 1e12) {
    return (numericValue / 1e12).toFixed(2) + 'T';
  } else if (Math.abs(numericValue) >= 1e9) {
    return (numericValue / 1e9).toFixed(2) + 'B';
  } else if (Math.abs(numericValue) >= 1e6) {
    return (numericValue / 1e6).toFixed(2) + 'M';
  } else if (Math.abs(numericValue) >= 1e3) {
    return (numericValue / 1e3).toFixed(2) + 'K';
  } else {
    return numericValue.toFixed(2);
  }
};

// Utility function to debounce function calls
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout;
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

// BigDecimal utility functions for handling string values from backend

/**
 * Safely parse a BigDecimal string value to number
 * @param value - BigDecimal string value from backend
 * @returns Parsed number or 0 if invalid
 */
export const parseBigDecimal = (value: string | null | undefined): number => {
  if (!value || value.trim() === '') {
    return 0;
  }
  
  try {
    const parsed = parseFloat(value);
    if (isNaN(parsed) || !isFinite(parsed)) {
      console.warn(`Invalid BigDecimal value: ${value}`);
      return 0;
    }
    return parsed;
  } catch (error) {
    console.error(`Error parsing BigDecimal value: ${value}`, error);
    return 0;
  }
};

/**
 * Parse an array of BigDecimal string values to numbers
 * @param values - Array of BigDecimal string values
 * @returns Array of parsed numbers
 */
export const parseBigDecimalArray = (values: string[] | null | undefined): number[] => {
  if (!values || !Array.isArray(values)) {
    return [];
  }
  
  return values.map(parseBigDecimal);
};

/**
 * Format a number as a BigDecimal string for sending to backend
 * @param value - Number value to format
 * @param decimalPlaces - Number of decimal places (default: 6)
 * @returns Formatted string value
 */
export const formatToBigDecimal = (value: number, decimalPlaces: number = 6): string => {
  if (isNaN(value) || !isFinite(value)) {
    return '0';
  }
  
  return value.toFixed(decimalPlaces);
};

/**
 * Validate that a string represents a valid BigDecimal value
 * @param value - String value to validate
 * @returns True if valid BigDecimal format
 */
export const isValidBigDecimal = (value: string): boolean => {
  if (!value || value.trim() === '') {
    return false;
  }
  
  // Check for valid decimal number format
  const decimalRegex = /^-?\d+(\.\d+)?$/;
  return decimalRegex.test(value.trim());
};

/**
 * Format BigDecimal string for display with proper decimal places
 * @param value - BigDecimal string value
 * @param decimalPlaces - Number of decimal places for display
 * @returns Formatted display string
 */
export const formatBigDecimalForDisplay = (value: string | null | undefined, decimalPlaces: number = 2): string => {
  const parsed = parseBigDecimal(value);
  return parsed.toFixed(decimalPlaces);
};