import apiClient from './authService';
import { FinancialData, DCFInput, DCFOutput, ApiResponse } from '../types';
import { API_ENDPOINTS } from '../config/api';
import { retryApiCall, extractErrorMessage, formatApiResponse } from '../utils/apiUtils';

export const financialService = {
  async getFinancialData(ticker: string): Promise<FinancialData> {
    if (!ticker || ticker.trim().length === 0) {
      throw new Error('Ticker symbol is required');
    }
    
    try {
      const response = await retryApiCall(() => 
        apiClient.get<any>(`${API_ENDPOINTS.FINANCIAL.GET_DATA}?ticker=${ticker.toUpperCase()}`)
      );
      
      const data = formatApiResponse<FinancialData>(response);
      
      // Validate the received data
      if (!data || !data.ticker) {
        throw new Error('Invalid financial data received');
      }
      
      return data;
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async calculateDCF(dcfInput: DCFInput): Promise<DCFOutput> {
    try {
      const response = await retryApiCall(() => 
        apiClient.post<ApiResponse<DCFOutput>>(API_ENDPOINTS.FINANCIAL.CALCULATE_DCF, dcfInput)
      );
      
      return formatApiResponse<DCFOutput>(response);
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }
};