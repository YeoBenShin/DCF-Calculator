import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import DCFCalculator from '../DCFCalculator';
import { financialService } from '../../../services/financialService';

// Mock the financial service
jest.mock('../../../services/financialService');
const mockFinancialService = financialService as jest.Mocked<typeof financialService>;

// Mock react-router-dom
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('DCFCalculator', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders all form fields and labels correctly', () => {
    renderWithRouter(<DCFCalculator />);
    
    expect(screen.getByText('DCF Calculator')).toBeInTheDocument();
    expect(screen.getByLabelText(/stock ticker symbol/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/discount rate/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /calculate fair value/i })).toBeInTheDocument();
    
    // Check that the specific input fields exist by ID
    expect(document.getElementById('growth_rate')).toBeInTheDocument();
    expect(document.getElementById('terminal_growth_rate')).toBeInTheDocument();
    
    // Check that the form sections are rendered
    expect(screen.getByText('Stock Information')).toBeInTheDocument();
    expect(screen.getByText('DCF Parameters')).toBeInTheDocument();
  });

  describe('Form Validation', () => {
    it('shows error when ticker is empty and form is submitted', async () => {
      // Mock successful financial data fetch to enable submit button
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: [100000],
        operating_expense: [50000],
        operating_income: [50000],
        operating_cash_flow: [55000],
        net_profit: [40000],
        capital_expenditure: [10000],
        free_cash_flow: [45000],
        eps: [5.5],
        total_debt: [120000],
        ordinary_shares_number: [8000],
        date_fetched: '2023-12-01'
      };
      
      mockFinancialService.getFinancialData.mockResolvedValue(mockFinancialData);
      
      renderWithRouter(<DCFCalculator />);
      
      // First search for financial data to enable submit button
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      fireEvent.click(searchButton);
      
      await waitFor(() => {
        expect(screen.getByText('✓ Financial data loaded for AAPL')).toBeInTheDocument();
      });
      
      // Clear ticker to test validation
      fireEvent.change(tickerInput, { target: { value: '' } });
      
      const submitButton = screen.getByRole('button', { name: /calculate fair value/i });
      fireEvent.click(submitButton);
      
      expect(screen.getByText('Stock ticker is required')).toBeInTheDocument();
    });

    it('shows error when ticker format is invalid', async () => {
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: [100000],
        operating_expense: [50000],
        operating_income: [50000],
        operating_cash_flow: [55000],
        net_profit: [40000],
        capital_expenditure: [10000],
        free_cash_flow: [45000],
        eps: [5.5],
        total_debt: [120000],
        ordinary_shares_number: [8000],
        date_fetched: '2023-12-01'
      };
      
      mockFinancialService.getFinancialData.mockResolvedValue(mockFinancialData);
      
      renderWithRouter(<DCFCalculator />);
      
      // First get valid financial data
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      fireEvent.click(searchButton);
      
      await waitFor(() => {
        expect(screen.getByText('✓ Financial data loaded for AAPL')).toBeInTheDocument();
      });
      
      // Change to invalid ticker format
      fireEvent.change(tickerInput, { target: { value: '123' } });
      
      const submitButton = screen.getByRole('button', { name: /calculate fair value/i });
      fireEvent.click(submitButton);
      
      expect(screen.getByText('Please enter a valid stock ticker (1-5 letters)')).toBeInTheDocument();
    });

    it('shows error when growth rate is too high', async () => {
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: [100000],
        operating_expense: [50000],
        operating_income: [50000],
        operating_cash_flow: [55000],
        net_profit: [40000],
        capital_expenditure: [10000],
        free_cash_flow: [45000],
        eps: [5.5],
        total_debt: [120000],
        ordinary_shares_number: [8000],
        date_fetched: '2023-12-01'
      };
      
      mockFinancialService.getFinancialData.mockResolvedValue(mockFinancialData);
      
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      const growthRateInput = document.getElementById('growth_rate') as HTMLInputElement;
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      fireEvent.click(searchButton);
      
      await waitFor(() => {
        expect(screen.getByText('✓ Financial data loaded for AAPL')).toBeInTheDocument();
      });
      
      fireEvent.change(growthRateInput, { target: { value: '1500' } });
      
      const submitButton = screen.getByRole('button', { name: /calculate fair value/i });
      fireEvent.click(submitButton);
      
      expect(screen.getByText('Growth rate too high. Please input a realistic value.')).toBeInTheDocument();
    });

    it('clears error when user starts typing in field', async () => {
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: [100000],
        operating_expense: [50000],
        operating_income: [50000],
        operating_cash_flow: [55000],
        net_profit: [40000],
        capital_expenditure: [10000],
        free_cash_flow: [45000],
        eps: [5.5],
        total_debt: [120000],
        ordinary_shares_number: [8000],
        date_fetched: '2023-12-01'
      };
      
      mockFinancialService.getFinancialData.mockResolvedValue(mockFinancialData);
      
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      fireEvent.click(searchButton);
      
      await waitFor(() => {
        expect(screen.getByText('✓ Financial data loaded for AAPL')).toBeInTheDocument();
      });
      
      // Clear ticker to trigger validation error
      fireEvent.change(tickerInput, { target: { value: '' } });
      
      const submitButton = screen.getByRole('button', { name: /calculate fair value/i });
      fireEvent.click(submitButton);
      
      expect(screen.getByText('Stock ticker is required')).toBeInTheDocument();
      
      // Start typing to clear error
      fireEvent.change(tickerInput, { target: { value: 'A' } });
      
      expect(screen.queryByText('Stock ticker is required')).not.toBeInTheDocument();
    });
  });

  describe('Stock Ticker Search', () => {
    it('disables search button when ticker is empty', () => {
      renderWithRouter(<DCFCalculator />);
      
      const searchButton = screen.getByRole('button', { name: /search/i });
      expect(searchButton).toBeDisabled();
    });

    it('enables search button when ticker is entered', () => {
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      expect(searchButton).not.toBeDisabled();
    });

    it('calls financial service when search button is clicked', async () => {
      const mockFinancialData = {
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
      
      mockFinancialService.getFinancialData.mockResolvedValue(mockFinancialData);
      
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'AAPL' } });
      fireEvent.click(searchButton);
      
      expect(mockFinancialService.getFinancialData).toHaveBeenCalledWith('AAPL');
      
      await waitFor(() => {
        expect(screen.getByText('✓ Financial data loaded for AAPL')).toBeInTheDocument();
      });
    });

    it('shows error when financial data fetch fails', async () => {
      mockFinancialService.getFinancialData.mockRejectedValue(new Error('Ticker not found.'));
      
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i);
      const searchButton = screen.getByRole('button', { name: /search/i });
      
      fireEvent.change(tickerInput, { target: { value: 'INVALID' } });
      fireEvent.click(searchButton);
      
      await waitFor(() => {
        expect(screen.getByText('Ticker not found.')).toBeInTheDocument();
      });
    });
  });

  describe('Input Formatting', () => {
    it('converts ticker to uppercase', () => {
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i) as HTMLInputElement;
      fireEvent.change(tickerInput, { target: { value: 'aapl' } });
      
      expect(tickerInput.value).toBe('AAPL');
    });

    it('limits ticker length to 5 characters', () => {
      renderWithRouter(<DCFCalculator />);
      
      const tickerInput = screen.getByLabelText(/stock ticker symbol/i) as HTMLInputElement;
      fireEvent.change(tickerInput, { target: { value: 'TOOLONG' } });
      
      expect(tickerInput.value).toBe('TOOLO');
    });
  });
});