import apiClient from './authService';
import { WatchlistItem, WatchlistRequest, ApiResponse } from '../types';
import { API_ENDPOINTS } from '../config/api';
import { retryApiCall, extractErrorMessage, formatApiResponse } from '../utils/apiUtils';

export const watchlistService = {
  async getWatchlist(): Promise<WatchlistItem[]> {
    try {
      const response = await retryApiCall(() => 
        apiClient.get<WatchlistItem[]>(API_ENDPOINTS.WATCHLIST.GET)
      );
      
      // The watchlist endpoint returns the array directly
      return response.data;
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async addToWatchlist(ticker: string): Promise<void> {
    try {
      const request: WatchlistRequest = { ticker: ticker.toUpperCase() };
      const response = await retryApiCall(() => 
        apiClient.post<any>(API_ENDPOINTS.WATCHLIST.ADD, request)
      );
      
      // The add endpoint returns a success message, no need to format
      if (response.data.error) {
        throw new Error(response.data.error);
      }
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async removeFromWatchlist(ticker: string): Promise<void> {
    try {
      const request: WatchlistRequest = { ticker: ticker.toUpperCase() };
      const response = await retryApiCall(() => 
        apiClient.delete<any>(API_ENDPOINTS.WATCHLIST.REMOVE, { data: request })
      );
      
      // The remove endpoint returns a success message, no need to format
      if (response.data.error) {
        throw new Error(response.data.error);
      }
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }
};