import apiClient from './authService';
import { WatchlistItem, WatchlistRequest, ApiResponse } from '../types';
import { API_ENDPOINTS } from '../config/api';
import { retryApiCall, extractErrorMessage, formatApiResponse } from '../utils/apiUtils';

export const watchlistService = {
  async getWatchlist(): Promise<WatchlistItem[]> {
    try {
      const response = await retryApiCall(() => 
        apiClient.get<ApiResponse<WatchlistItem[]>>(API_ENDPOINTS.WATCHLIST.GET)
      );
      
      return formatApiResponse<WatchlistItem[]>(response);
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async addToWatchlist(ticker: string): Promise<void> {
    try {
      const request: WatchlistRequest = { ticker: ticker.toUpperCase() };
      const response = await retryApiCall(() => 
        apiClient.post<ApiResponse<void>>(API_ENDPOINTS.WATCHLIST.ADD, request)
      );
      
      formatApiResponse<void>(response);
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async removeFromWatchlist(ticker: string): Promise<void> {
    try {
      const response = await retryApiCall(() => 
        apiClient.delete<ApiResponse<void>>(`${API_ENDPOINTS.WATCHLIST.REMOVE}?ticker=${ticker.toUpperCase()}`)
      );
      
      formatApiResponse<void>(response);
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }
};