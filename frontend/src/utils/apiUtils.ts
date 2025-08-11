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
  if (error.response?.data?.error) {
    return error.response.data.error;
  }
  
  if (error.response?.status) {
    switch (error.response.status) {
      case 400:
        return ERROR_MESSAGES.VALIDATION_ERROR;
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
  if (response.data.success && response.data.data !== undefined) {
    return response.data.data;
  }
  
  const error = new Error(response.data.error || 'API response format error');
  throw error;
};

// Utility function to validate ticker symbol
export const validateTicker = (ticker: string): boolean => {
  // Basic ticker validation: 1-5 uppercase letters
  const tickerRegex = /^[A-Z]{1,5}$/;
  return tickerRegex.test(ticker.toUpperCase());
};

// Utility function to format currency
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

// Utility function to format percentage
export const formatPercentage = (value: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value / 100);
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