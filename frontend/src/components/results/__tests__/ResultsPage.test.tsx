import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter, MemoryRouter } from 'react-router-dom';
import ResultsPage from '../ResultsPage';
import { DCFOutput, FinancialData, DCFInput } from '../../../types';

// Mock the navigate function
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock the FinancialCharts component to avoid Recharts issues in tests
jest.mock('../FinancialCharts', () => {
  return function MockFinancialCharts({ financialData }: { financialData: any }) {
    return (
      <div data-testid="financial-charts">
        <h2>Financial Performance</h2>
        <p>Data for {financialData.ticker}</p>
      </div>
    );
  };
});

// Mock data
const mockDCFResult: DCFOutput = {
  ticker: 'AAPL',
  fair_value_per_share: 175.50,
  current_price: 150.00,
  valuation: 'Undervalued'
};

const mockFinancialData: FinancialData = {
  ticker: 'AAPL',
  revenue: [100000000000, 110000000000, 120000000000],
  operating_expense: [80000000000, 85000000000, 90000000000],
  operating_income: [20000000000, 25000000000, 30000000000],
  operating_cash_flow: [25000000000, 30000000000, 35000000000],
  net_profit: [15000000000, 20000000000, 25000000000],
  capital_expenditure: [5000000000, 6000000000, 7000000000],
  free_cash_flow: [20000000000, 24000000000, 28000000000],
  eps: [3.50, 4.25, 5.00],
  total_debt: [50000000000, 55000000000, 60000000000],
  ordinary_shares_number: [5000000000, 4800000000, 4600000000],
  date_fetched: '2025-01-01'
};

const mockDCFInput: DCFInput = {
  ticker: 'AAPL',
  discountRate: 10,
  growthRate: 15,
  terminalGrowthRate: 2.5
};

const mockLocationState = {
  dcfResult: mockDCFResult,
  financialData: mockFinancialData,
  dcfInput: mockDCFInput
};

// Helper function to render with router and state
const renderWithRouter = (state: any = mockLocationState) => {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/results', state }]}>
      <ResultsPage />
    </MemoryRouter>
  );
};

describe('ResultsPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('renders results page with all components when data is available', () => {
    renderWithRouter();

    // Check header
    expect(screen.getByText('DCF Analysis Results')).toBeInTheDocument();
    expect(screen.getByText('AAPL')).toBeInTheDocument();

    // Check fair value card
    expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
    expect(screen.getByText('$175.50')).toBeInTheDocument();
    expect(screen.getByText('$150.00')).toBeInTheDocument();
    expect(screen.getByText('Undervalued')).toBeInTheDocument();

    // Check financial charts
    expect(screen.getByText('Financial Performance')).toBeInTheDocument();

    // Check action buttons
    expect(screen.getByText('New Calculation')).toBeInTheDocument();
    expect(screen.getByText('Add to Watchlist')).toBeInTheDocument();
  });

  it('redirects to calculator when no data is available', () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/results', state: null }]}>
        <ResultsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Redirecting to calculator...')).toBeInTheDocument();
    expect(mockNavigate).toHaveBeenCalledWith('/calculator');
  });

  it('redirects to calculator when dcfResult is missing', () => {
    const incompleteState = {
      financialData: mockFinancialData,
      dcfInput: mockDCFInput
    };

    render(
      <MemoryRouter initialEntries={[{ pathname: '/results', state: incompleteState }]}>
        <ResultsPage />
      </MemoryRouter>
    );

    expect(mockNavigate).toHaveBeenCalledWith('/calculator');
  });

  it('redirects to calculator when financialData is missing', () => {
    const incompleteState = {
      dcfResult: mockDCFResult,
      dcfInput: mockDCFInput
    };

    render(
      <MemoryRouter initialEntries={[{ pathname: '/results', state: incompleteState }]}>
        <ResultsPage />
      </MemoryRouter>
    );

    expect(mockNavigate).toHaveBeenCalledWith('/calculator');
  });

  it('handles new calculation button click', () => {
    renderWithRouter();

    const newCalculationButton = screen.getByText('New Calculation');
    fireEvent.click(newCalculationButton);

    expect(mockNavigate).toHaveBeenCalledWith('/calculator');
  });

  it('handles add to watchlist button click', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    renderWithRouter();

    const addToWatchlistButton = screen.getByText('Add to Watchlist');
    fireEvent.click(addToWatchlistButton);

    expect(consoleSpy).toHaveBeenCalledWith('Add to watchlist functionality coming soon');
    consoleSpy.mockRestore();
  });

  it('displays loading state correctly', () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/results', state: null }]}>
        <ResultsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Redirecting to calculator...')).toBeInTheDocument();
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });
});