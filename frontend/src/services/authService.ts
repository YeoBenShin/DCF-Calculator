import axios from 'axios';
import { AuthRequest, AuthResponse, ApiResponse } from '../types';
import { API_CONFIG, API_ENDPOINTS, ERROR_MESSAGES, APP_CONFIG } from '../config/api';
import { retryApiCall, extractErrorMessage, formatApiResponse } from '../utils/apiUtils';
import { error } from 'console';

// Global error handler function - will be set by App component
let globalErrorHandler: ((error: string) => void) | null = null;

export const setGlobalHandlers = (
  errorHandler: (error: string) => void,
  _loadingHandler: (isLoading: boolean, message?: string) => void
) => {
  globalErrorHandler = errorHandler;
  // Loading handler is available but not used in this service
};

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(APP_CONFIG.TOKEN_STORAGE_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle common errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid, clear local storage
      localStorage.removeItem(APP_CONFIG.TOKEN_STORAGE_KEY);
      localStorage.removeItem(APP_CONFIG.USER_STORAGE_KEY);
      
      // Use global error handler if available
      if (globalErrorHandler) {
        globalErrorHandler(ERROR_MESSAGES.UNAUTHORIZED);
      }
      
      // Redirect to login
      window.location.href = '/login';
    } else if (error.response?.status >= 500) {
      // Server errors
      if (globalErrorHandler) {
        globalErrorHandler(ERROR_MESSAGES.SERVER_ERROR);
      }
    } else if (!error.response) {
      // Network errors
      if (globalErrorHandler) {
        globalErrorHandler(ERROR_MESSAGES.NETWORK_ERROR);
      }
    }
    
    return Promise.reject(error);
  }
);

export const authService = {
  async login(credentials: AuthRequest): Promise<AuthResponse> {
    try {
      const response = await retryApiCall(() => 
        apiClient.post<ApiResponse<AuthResponse>>(API_ENDPOINTS.AUTH.LOGIN, credentials)
      );
      return {...formatApiResponse<AuthResponse>(response)};
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async signup(userData: AuthRequest): Promise<AuthResponse> {
    try {
      const response = await retryApiCall(() => 
        apiClient.post<ApiResponse<AuthResponse>>(API_ENDPOINTS.AUTH.SIGNUP, userData)
      );
      
      return formatApiResponse<AuthResponse>(response);
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  logout(): void {
    localStorage.removeItem(APP_CONFIG.TOKEN_STORAGE_KEY);
    localStorage.removeItem(APP_CONFIG.USER_STORAGE_KEY);
  },

  getCurrentUser(): any {
    const userStr = localStorage.getItem(APP_CONFIG.USER_STORAGE_KEY);
    return userStr ? JSON.parse(userStr) : null;
  },

  getToken(): string | null {
    return localStorage.getItem(APP_CONFIG.TOKEN_STORAGE_KEY);
  },

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
};

export default apiClient;