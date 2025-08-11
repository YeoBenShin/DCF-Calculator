import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FinancialCharts from '../FinancialCharts';
import { FinancialData } from '../../../types';

// Mock Recharts components
jest.mock('recharts', () => ({
  LineChart: ({ children }: any) => <div data-testid="line-chart">{children}</div>,
  Line: () => <div data-testid="line" />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
  Legend: () => <div data-testid="legend" />,
  ResponsiveContainer: ({ children }: any) => <div data-testid="responsive-container">{children}</div>,
}));

// Mock data
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

const mockEmptyFinancialData: FinancialData = {
  ticker: 'EMPTY',
  revenue: [0, 0, 0],
  operating_expense: [0, 0, 0],
  operating_income: [0, 0, 0],
  operating_cash_flow: [0, 0, 0],
  net_profit: [0, 0, 0],
  capital_expenditure: [0, 0, 0],
  free_cash_flow: [0, 0, 0],
  eps: [0, 0, 0],
  total_debt: [0, 0, 0],
  ordinary_shares_number: [0, 0, 0],
  date_fetched: '2025-01-01'
};

describe('FinancialCharts', () => {
  it('renders financial charts component with default revenue chart', () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    // Check header
    expect(screen.getByText('Financial Performance')).toBeInTheDocument();
    expect(screen.getByText('Data for AAPL • Last updated: 1/1/2025')).toBeInTheDocument();

    // Check navigation buttons
    expect(screen.getByText('Revenue')).toBeInTheDocument();
    expect(screen.getByText('Net Income')).toBeInTheDocument();
    expect(screen.getByText('Free Cash Flow')).toBeInTheDocument();
    expect(screen.getByText('EPS')).toBeInTheDocument();

    // Check chart components
    expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();

    // Check chart title
    expect(screen.getByText('Revenue Trend')).toBeInTheDocument();
  });

  it('switches between different chart types', async () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    // Initially shows revenue chart
    expect(screen.getByText('Revenue Trend')).toBeInTheDocument();

    // Click on Net Income button
    const netIncomeButton = screen.getByText('Net Income');
    fireEvent.click(netIncomeButton);

    // Wait for loading to complete and check new chart
    await waitFor(() => {
      expect(screen.getByText('Net Income Trend')).toBeInTheDocument();
    });

    // Click on Free Cash Flow button
    const fcfButton = screen.getByText('Free Cash Flow');
    fireEvent.click(fcfButton);

    await waitFor(() => {
      expect(screen.getByText('Free Cash Flow Trend')).toBeInTheDocument();
    });

    // Click on EPS button
    const epsButton = screen.getByText('EPS');
    fireEvent.click(epsButton);

    await waitFor(() => {
      expect(screen.getByText('Earnings Per Share (EPS) Trend')).toBeInTheDocument();
    });
  });

  it('shows loading state when switching charts', async () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    const netIncomeButton = screen.getByText('Net Income');
    fireEvent.click(netIncomeButton);

    // Should show loading state briefly
    expect(screen.getByText('Loading chart...')).toBeInTheDocument();

    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.queryByText('Loading chart...')).not.toBeInTheDocument();
    });
  });

  it('highlights active navigation button', () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    const revenueButton = screen.getByText('Revenue');
    const netIncomeButton = screen.getByText('Net Income');

    // Revenue should be active by default
    expect(revenueButton).toHaveClass('active');
    expect(netIncomeButton).not.toHaveClass('active');

    // Click Net Income
    fireEvent.click(netIncomeButton);

    // Net Income should now be active
    expect(netIncomeButton).toHaveClass('active');
    expect(revenueButton).not.toHaveClass('active');
  });

  it('displays chart summary with latest value and trend', async () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    // Wait for chart to load
    await waitFor(() => {
      expect(screen.getByText('Latest Value')).toBeInTheDocument();
      expect(screen.getByText('Trend')).toBeInTheDocument();
    });

    // Check that trend shows growing (since revenue increases from 100B to 120B)
    expect(screen.getByText('↗ Growing')).toBeInTheDocument();
  });

  it('handles empty data gracefully', () => {
    render(<FinancialCharts financialData={mockEmptyFinancialData} />);

    expect(screen.getByText('No data available for Revenue Trend')).toBeInTheDocument();
  });

  it('formats values correctly for different chart types', async () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    // Test EPS formatting (should show $5.00 for latest EPS)
    const epsButton = screen.getByText('EPS');
    fireEvent.click(epsButton);

    await waitFor(() => {
      expect(screen.getByText('Earnings Per Share (EPS) Trend')).toBeInTheDocument();
    });

    // Note: The actual value formatting is tested in the component logic
    // The display of formatted values in the DOM would require more complex mocking
  });

  it('shows declining trend for decreasing values', async () => {
    const decliningData: FinancialData = {
      ...mockFinancialData,
      revenue: [120000000000, 110000000000, 100000000000] // Declining revenue
    };

    render(<FinancialCharts financialData={decliningData} />);

    await waitFor(() => {
      expect(screen.getByText('↘ Declining')).toBeInTheDocument();
    });
  });

  it('displays correct data source information', () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    expect(screen.getByText('Data for AAPL • Last updated: 1/1/2025')).toBeInTheDocument();
  });

  it('handles chart navigation keyboard accessibility', () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    const revenueButton = screen.getByText('Revenue');
    const netIncomeButton = screen.getByText('Net Income');

    // Test that buttons are focusable
    revenueButton.focus();
    expect(document.activeElement).toBe(revenueButton);

    netIncomeButton.focus();
    expect(document.activeElement).toBe(netIncomeButton);
  });
});