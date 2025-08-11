import { financialService } from '../financialService';
import apiClient from '../authService';
import { FinancialData, DCFInput, DCFOutput } from '../../types';

// Mock the API client
jest.mock('../authService');
const mockApiClient = apiClient as jest.Mocked<typeof apiClient>;

describe('financialService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getFinancialData', () => {
    const mockFinancialData: FinancialData = {
      ticker: 'AAPL',
      revenue: [100000, 95000, 90000],
      operating_expense: [50000, 48000, 45000],
      operating_income: [50000, 47000, 45000],
      operating_cash_flow: [55000, 52000, 50000],
      net_profit: [40000, 38000, 36000],
      capital_expenditure: [10000, 9500, 9000],
      free_cash_flow: [45000, 42500, 41000],
      eps: [5.5, 5.2, 4.8],
      total_debt: [120000, 115000, 110000],
      ordinary_shares_number: [8000, 8100, 8200],
      date_fetched: '2023-12-01'
    };

    it('should fetch financial data successfully', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          success: true,
          data: mockFinancialData
        }
      });

      const result = await financialService.getFinancialData('AAPL');

      expect(mockApiClient.get).toHaveBeenCalledWith('/financials?ticker=AAPL');
      expect(result).toEqual(mockFinancialData);
    });

    it('should convert ticker to uppercase', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          success: true,
          data: mockFinancialData
        }
      });

      await financialService.getFinancialData('aapl');

      expect(mockApiClient.get).toHaveBeenCalledWith('/financials?ticker=AAPL');
    });

    it('should throw error when API returns error', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          success: false,
          error: 'Invalid ticker symbol'
        }
      });

      await expect(financialService.getFinancialData('INVALID')).rejects.toThrow('Invalid ticker symbol');
    });

    it('should throw "Ticker not found." for 404 errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 404,
          data: { error: 'Not found' }
        }
      });

      await expect(financialService.getFinancialData('NOTFOUND')).rejects.toThrow('Ticker not found.');
    });

    it('should throw "Unable to retrieve financials at the moment." for server errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 500,
          data: { error: 'Internal server error' }
        }
      });

      await expect(financialService.getFinancialData('AAPL')).rejects.toThrow('Unable to retrieve financials at the moment.');
    });

    it('should throw network error for connection issues', async () => {
      mockApiClient.get.mockRejectedValue(new Error('Network Error'));

      await expect(financialService.getFinancialData('AAPL')).rejects.toThrow('Network error. Please check your connection.');
    });

    it('should throw custom error message from response', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 400,
          data: { error: 'Custom error message' }
        }
      });

      await expect(financialService.getFinancialData('AAPL')).rejects.toThrow('Custom error message');
    });
  });

  describe('calculateDCF', () => {
    const mockDCFInput: DCFInput = {
      ticker: 'AAPL',
      discount_rate: 10,
      growth_rate: 15,
      terminal_growth_rate: 2.5
    };

    const mockDCFOutput: DCFOutput = {
      ticker: 'AAPL',
      fair_value_per_share: 175.50,
      current_price: 150.00,
      valuation: 'Undervalued'
    };

    it('should calculate DCF successfully', async () => {
      mockApiClient.post.mockResolvedValue({
        data: {
          success: true,
          data: mockDCFOutput
        }
      });

      const result = await financialService.calculateDCF(mockDCFInput);

      expect(mockApiClient.post).toHaveBeenCalledWith('/calculateDCF', mockDCFInput);
      expect(result).toEqual(mockDCFOutput);
    });

    it('should throw error when API returns error', async () => {
      mockApiClient.post.mockResolvedValue({
        data: {
          success: false,
          error: 'Invalid input parameters'
        }
      });

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('Invalid input parameters');
    });

    it('should throw server error for 500 status', async () => {
      mockApiClient.post.mockRejectedValue({
        response: {
          status: 500,
          data: { error: 'Internal server error' }
        }
      });

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('Server error. Please try again later.');
    });

    it('should throw network error for connection issues', async () => {
      mockApiClient.post.mockRejectedValue(new Error('Network Error'));

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('Network error. Please check your connection.');
    });

    it('should throw custom error message from response', async () => {
      mockApiClient.post.mockRejectedValue({
        response: {
          status: 400,
          data: { error: 'Growth rate too high' }
        }
      });

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('Growth rate too high');
    });

    it('should throw default error when no specific error message', async () => {
      mockApiClient.post.mockResolvedValue({
        data: {
          success: false
        }
      });

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('DCF calculation failed');
    });
  });
});