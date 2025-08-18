import apiClient from './authService';
import { FinancialData, DCFInput, DCFOutput, ApiResponse } from '../types';
import { API_ENDPOINTS } from '../config/api';
import { 
  retryApiCall, 
  extractErrorMessage, 
  formatApiResponse,
  parseBigDecimal,
  parseBigDecimalArray,
  formatToBigDecimal,
  isValidBigDecimal
} from '../utils/apiUtils';

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
      
      // Validate BigDecimal arrays from backend
      this.validateFinancialDataBigDecimals(data);
      
      return data;
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  async calculateDCF(dcfInput: DCFInput): Promise<DCFOutput> {
    try {
      // Validate input BigDecimal values before sending to backend
      this.validateDCFInputBigDecimals(dcfInput);
      
      const response = await retryApiCall(() => 
        apiClient.post<ApiResponse<DCFOutput>>(API_ENDPOINTS.FINANCIAL.CALCULATE_DCF, dcfInput)
      );
      
      const result = formatApiResponse<DCFOutput>(response);
      
      // Validate output BigDecimal values from backend
      this.validateDCFOutputBigDecimals(result);
      
      return result;
    } catch (error: any) {
      const errorMessage = extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  },

  /**
   * Create DCF input with proper BigDecimal formatting
   */
  createDCFInput(ticker: string, discountRate: number, growthRate: number, terminalGrowthRate: number): DCFInput {
    return {
      ticker: ticker.toUpperCase(),
      discountRate: formatToBigDecimal(discountRate, 6),
      growthRate: formatToBigDecimal(growthRate, 6),
      terminalGrowthRate: formatToBigDecimal(terminalGrowthRate, 6)
    };
  },

  /**
   * Parse DCF output BigDecimal values for display
   */
  parseDCFOutputForDisplay(dcfOutput: DCFOutput) {
    return {
      ticker: dcfOutput.ticker,
      fairValuePerShare: parseBigDecimal(dcfOutput.fairValuePerShare),
      currentPrice: parseBigDecimal(dcfOutput.currentPrice),
      valuation: dcfOutput.valuation,
      upsideDownsidePercentage: dcfOutput.upsideDownsidePercentage ? parseBigDecimal(dcfOutput.upsideDownsidePercentage) : undefined,
      terminalValue: dcfOutput.terminalValue ? parseBigDecimal(dcfOutput.terminalValue) : undefined,
      presentValueOfCashFlows: dcfOutput.presentValueOfCashFlows ? parseBigDecimal(dcfOutput.presentValueOfCashFlows) : undefined,
      enterpriseValue: dcfOutput.enterpriseValue ? parseBigDecimal(dcfOutput.enterpriseValue) : undefined,
      equityValue: dcfOutput.equityValue ? parseBigDecimal(dcfOutput.equityValue) : undefined,
      sharesOutstanding: dcfOutput.sharesOutstanding ? parseBigDecimal(dcfOutput.sharesOutstanding) : undefined
    };
  },

  /**
   * Parse financial data BigDecimal arrays for display/charting
   */
  parseFinancialDataForDisplay(financialData: FinancialData) {
    return {
      ticker: financialData.ticker,
      revenue: parseBigDecimalArray(financialData.revenue),
      operating_expense: parseBigDecimalArray(financialData.operating_expense),
      operating_income: parseBigDecimalArray(financialData.operating_income),
      operating_cash_flow: parseBigDecimalArray(financialData.operating_cash_flow),
      net_profit: parseBigDecimalArray(financialData.net_profit),
      capital_expenditure: parseBigDecimalArray(financialData.capital_expenditure),
      free_cash_flow: parseBigDecimalArray(financialData.free_cash_flow),
      eps: parseBigDecimalArray(financialData.eps),
      total_debt: parseBigDecimalArray(financialData.total_debt),
      ordinary_shares_number: parseBigDecimalArray(financialData.ordinary_shares_number),
      date_fetched: financialData.date_fetched
    };
  },

  /**
   * Validate BigDecimal values in financial data
   */
  validateFinancialDataBigDecimals(data: FinancialData): void {
    const arrayFields = [
      'revenue', 'operating_expense', 'operating_income', 'operating_cash_flow',
      'net_profit', 'capital_expenditure', 'free_cash_flow', 'eps', 
      'total_debt', 'ordinary_shares_number'
    ];

    for (const field of arrayFields) {
      const values = (data as any)[field];
      if (values && Array.isArray(values)) {
        for (let i = 0; i < values.length; i++) {
          if (values[i] !== null && values[i] !== undefined && !isValidBigDecimal(values[i])) {
            throw new Error(`Invalid BigDecimal value in ${field}[${i}]: ${values[i]}`);
          }
        }
      }
    }
  },

  /**
   * Validate BigDecimal values in DCF input
   */
  validateDCFInputBigDecimals(input: DCFInput): void {
    if (!isValidBigDecimal(input.discountRate)) {
      throw new Error(`Invalid discount rate: ${input.discountRate}`);
    }
    if (!isValidBigDecimal(input.growthRate)) {
      throw new Error(`Invalid growth rate: ${input.growthRate}`);
    }
    if (!isValidBigDecimal(input.terminalGrowthRate)) {
      throw new Error(`Invalid terminal growth rate: ${input.terminalGrowthRate}`);
    }
  },

  /**
   * Validate BigDecimal values in DCF output
   */
  validateDCFOutputBigDecimals(output: DCFOutput): void {
    if (!isValidBigDecimal(output.fairValuePerShare)) {
      throw new Error(`Invalid fair value per share: ${output.fairValuePerShare}`);
    }
    if (!isValidBigDecimal(output.currentPrice)) {
      throw new Error(`Invalid current price: ${output.currentPrice}`);
    }
    
    // Optional fields validation
    const optionalFields = [
      'upsideDownsidePercentage', 'terminalValue', 'presentValueOfCashFlows',
      'enterpriseValue', 'equityValue', 'sharesOutstanding'
    ];
    
    for (const field of optionalFields) {
      const value = (output as any)[field];
      if (value !== null && value !== undefined && !isValidBigDecimal(value)) {
        throw new Error(`Invalid ${field}: ${value}`);
      }
    }
  }
};