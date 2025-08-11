import apiClient from './authService';
import { FinancialData, DCFInput, DCFOutput, ApiResponse } from '../types';
import { API_ENDPOINTS } from '../config/api';
import { retryApiCall, extractErrorMessage, formatApiResponse } from '../utils/apiUtils';

export const financialService = {
  async getFinancialData(ticker: string): Promise<FinancialData> {
    try {
      const response = await retryApiCall(() => 
        apiClient.get<ApiResponse<FinancialData>>(`${API_ENDPOINTS.FINANCIAL.GET_DATA}?ticker=${ticker.toUpperCase()}`)
      );
      
      return formatApiResponse<FinancialData>(response);
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