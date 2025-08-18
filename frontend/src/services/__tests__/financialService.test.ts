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
      revenue: ['100000.00', '95000.00', '90000.00'],
      operating_expense: ['50000.00', '48000.00', '45000.00'],
      operating_income: ['50000.00', '47000.00', '45000.00'],
      operating_cash_flow: ['55000.00', '52000.00', '50000.00'],
      net_profit: ['40000.00', '38000.00', '36000.00'],
      capital_expenditure: ['10000.00', '9500.00', '9000.00'],
      free_cash_flow: ['45000.00', '42500.00', '41000.00'],
      eps: ['5.50', '5.20', '4.80'],
      total_debt: ['120000.00', '115000.00', '110000.00'],
      ordinary_shares_number: ['8000.00', '8100.00', '8200.00'],
      date_fetched: '2023-12-01'
    };

    it('should fetch financial data successfully', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          success: true,
          data: mockFinancialData
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
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
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
      });

      await financialService.getFinancialData('aapl');

      expect(mockApiClient.get).toHaveBeenCalledWith('/financials?ticker=AAPL');
    });

    it('should throw error when API returns error', async () => {
      mockApiClient.get.mockResolvedValue({
        data: {
          error: 'Invalid ticker symbol'
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
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

      await expect(financialService.getFinancialData('NOTFOUND')).rejects.toThrow('Not found');
    });

    it('should throw "Unable to retrieve financials at the moment." for server errors', async () => {
      mockApiClient.get.mockRejectedValue({
        response: {
          status: 500,
          data: { error: 'Internal server error' }
        }
      });

      await expect(financialService.getFinancialData('AAPL')).rejects.toThrow('Internal server error');
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
      discountRate: '10.000000',
      growthRate: '15.000000',
      terminalGrowthRate: '2.500000'
    };

    const mockDCFOutput: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.50',
      currentPrice: '150.00',
      valuation: 'Undervalued'
    };

    it('should calculate DCF successfully', async () => {
      mockApiClient.post.mockResolvedValue({
        data: {
          success: true,
          data: mockDCFOutput
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
      });

      const result = await financialService.calculateDCF(mockDCFInput);

      expect(mockApiClient.post).toHaveBeenCalledWith('/dcf/calculate', mockDCFInput);
      expect(result).toEqual(mockDCFOutput);
    });

    it('should throw error when API returns error', async () => {
      mockApiClient.post.mockResolvedValue({
        data: {
          error: 'Invalid input parameters'
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
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

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('Internal server error');
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
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {}
      });

      await expect(financialService.calculateDCF(mockDCFInput)).rejects.toThrow('An unexpected error occurred');
    });
  });

  describe('BigDecimal utility functions', () => {
    describe('createDCFInput', () => {
      it('should create DCF input with proper BigDecimal formatting', () => {
        const result = financialService.createDCFInput('aapl', 10.5, 15.25, 2.75);
        
        expect(result).toEqual({
          ticker: 'AAPL',
          discountRate: '10.500000',
          growthRate: '15.250000',
          terminalGrowthRate: '2.750000'
        });
      });
    });

    describe('parseDCFOutputForDisplay', () => {
      it('should parse DCF output BigDecimal values for display', () => {
        const dcfOutput: DCFOutput = {
          ticker: 'AAPL',
          fairValuePerShare: '175.50',
          currentPrice: '150.00',
          valuation: 'Undervalued',
          upsideDownsidePercentage: '17.00',
          terminalValue: '1000000.00'
        };

        const result = financialService.parseDCFOutputForDisplay(dcfOutput);
        
        expect(result.fairValuePerShare).toBe(175.5);
        expect(result.currentPrice).toBe(150);
        expect(result.upsideDownsidePercentage).toBe(17);
        expect(result.terminalValue).toBe(1000000);
      });
    });

    describe('parseFinancialDataForDisplay', () => {
      it('should parse financial data BigDecimal arrays for display', () => {
        const financialData: FinancialData = {
          ticker: 'AAPL',
          revenue: ['100000.00', '95000.00'],
          operating_expense: ['50000.00', '48000.00'],
          operating_income: ['50000.00', '47000.00'],
          operating_cash_flow: ['55000.00', '52000.00'],
          net_profit: ['40000.00', '38000.00'],
          capital_expenditure: ['10000.00', '9500.00'],
          free_cash_flow: ['45000.00', '42500.00'],
          eps: ['5.50', '5.20'],
          total_debt: ['120000.00', '115000.00'],
          ordinary_shares_number: ['8000.00', '8100.00'],
          date_fetched: '2023-12-01'
        };

        const result = financialService.parseFinancialDataForDisplay(financialData);
        
        expect(result.revenue).toEqual([100000, 95000]);
        expect(result.eps).toEqual([5.5, 5.2]);
        expect(result.ticker).toBe('AAPL');
      });
    });

    describe('validation functions', () => {
      it('should validate financial data BigDecimals', () => {
        const validData: FinancialData = {
          ticker: 'AAPL',
          revenue: ['100000.00', '95000.00'],
          operating_expense: ['50000.00', '48000.00'],
          operating_income: ['50000.00', '47000.00'],
          operating_cash_flow: ['55000.00', '52000.00'],
          net_profit: ['40000.00', '38000.00'],
          capital_expenditure: ['10000.00', '9500.00'],
          free_cash_flow: ['45000.00', '42500.00'],
          eps: ['5.50', '5.20'],
          total_debt: ['120000.00', '115000.00'],
          ordinary_shares_number: ['8000.00', '8100.00'],
          date_fetched: '2023-12-01'
        };

        expect(() => financialService.validateFinancialDataBigDecimals(validData)).not.toThrow();
      });

      it('should throw error for invalid BigDecimal in financial data', () => {
        const invalidData: FinancialData = {
          ticker: 'AAPL',
          revenue: ['invalid', '95000.00'],
          operating_expense: ['50000.00', '48000.00'],
          operating_income: ['50000.00', '47000.00'],
          operating_cash_flow: ['55000.00', '52000.00'],
          net_profit: ['40000.00', '38000.00'],
          capital_expenditure: ['10000.00', '9500.00'],
          free_cash_flow: ['45000.00', '42500.00'],
          eps: ['5.50', '5.20'],
          total_debt: ['120000.00', '115000.00'],
          ordinary_shares_number: ['8000.00', '8100.00'],
          date_fetched: '2023-12-01'
        };

        expect(() => financialService.validateFinancialDataBigDecimals(invalidData)).toThrow('Invalid BigDecimal value in revenue[0]: invalid');
      });

      it('should validate DCF input BigDecimals', () => {
        const validInput: DCFInput = {
          ticker: 'AAPL',
          discountRate: '10.000000',
          growthRate: '15.000000',
          terminalGrowthRate: '2.500000'
        };

        expect(() => financialService.validateDCFInputBigDecimals(validInput)).not.toThrow();
      });

      it('should throw error for invalid DCF input BigDecimal', () => {
        const invalidInput: DCFInput = {
          ticker: 'AAPL',
          discountRate: 'invalid',
          growthRate: '15.000000',
          terminalGrowthRate: '2.500000'
        };

        expect(() => financialService.validateDCFInputBigDecimals(invalidInput)).toThrow('Invalid discount rate: invalid');
      });
    });
  });
});