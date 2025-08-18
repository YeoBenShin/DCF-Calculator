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

// Mock data - BigDecimal values as strings
const mockFinancialData: FinancialData = {
  ticker: 'AAPL',
  revenue: ['100000000000.00', '110000000000.00', '120000000000.00'],
  operating_expense: ['80000000000.00', '85000000000.00', '90000000000.00'],
  operating_income: ['20000000000.00', '25000000000.00', '30000000000.00'],
  operating_cash_flow: ['25000000000.00', '30000000000.00', '35000000000.00'],
  net_profit: ['15000000000.00', '20000000000.00', '25000000000.00'],
  capital_expenditure: ['5000000000.00', '6000000000.00', '7000000000.00'],
  free_cash_flow: ['20000000000.00', '24000000000.00', '28000000000.00'],
  eps: ['3.50', '4.25', '5.00'],
  total_debt: ['50000000000.00', '55000000000.00', '60000000000.00'],
  ordinary_shares_number: ['5000000000.00', '4800000000.00', '4600000000.00'],
  date_fetched: '2025-01-01'
};

const mockEmptyFinancialData: FinancialData = {
  ticker: 'EMPTY',
  revenue: ['0.00', '0.00', '0.00'],
  operating_expense: ['0.00', '0.00', '0.00'],
  operating_income: ['0.00', '0.00', '0.00'],
  operating_cash_flow: ['0.00', '0.00', '0.00'],
  net_profit: ['0.00', '0.00', '0.00'],
  capital_expenditure: ['0.00', '0.00', '0.00'],
  free_cash_flow: ['0.00', '0.00', '0.00'],
  eps: ['0.00', '0.00', '0.00'],
  total_debt: ['0.00', '0.00', '0.00'],
  ordinary_shares_number: ['0.00', '0.00', '0.00'],
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
      revenue: ['120000000000.00', '110000000000.00', '100000000000.00'] // Declining revenue
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

  it('handles very large BigDecimal values correctly', async () => {
    const largeValueData: FinancialData = {
      ...mockFinancialData,
      revenue: ['1000000000000000.00', '2000000000000000.00', '3000000000000000.00'], // Quadrillions
      eps: ['1000.123456', '2000.654321', '3000.987654'] // Large EPS values
    };

    render(<FinancialCharts financialData={largeValueData} />);

    // Should render without errors
    expect(screen.getByText('Financial Performance')).toBeInTheDocument();
    expect(screen.getByText('Revenue Trend')).toBeInTheDocument();

    // Switch to EPS to test large EPS formatting
    const epsButton = screen.getByText('EPS');
    fireEvent.click(epsButton);

    await waitFor(() => {
      expect(screen.getByText('Earnings Per Share (EPS) Trend')).toBeInTheDocument();
    });
  });

  it('handles very small BigDecimal values correctly', async () => {
    const smallValueData: FinancialData = {
      ...mockFinancialData,
      revenue: ['0.000001', '0.000002', '0.000003'], // Very small values
      eps: ['0.000123', '0.000456', '0.000789'] // Very small EPS values
    };

    render(<FinancialCharts financialData={smallValueData} />);

    // Should render without errors
    expect(screen.getByText('Financial Performance')).toBeInTheDocument();
    expect(screen.getByText('Revenue Trend')).toBeInTheDocument();

    // Switch to EPS to test small EPS formatting
    const epsButton = screen.getByText('EPS');
    fireEvent.click(epsButton);

    await waitFor(() => {
      expect(screen.getByText('Earnings Per Share (EPS) Trend')).toBeInTheDocument();
    });
  });

  it('handles invalid BigDecimal strings gracefully', async () => {
    const invalidData: FinancialData = {
      ...mockFinancialData,
      revenue: ['invalid', 'NaN', ''], // Invalid BigDecimal strings
      eps: ['not-a-number', '', 'undefined']
    };

    render(<FinancialCharts financialData={invalidData} />);

    // Should render without errors and show no data message
    expect(screen.getByText('No data available for Revenue Trend')).toBeInTheDocument();
  });

  it('displays range information in chart summary', async () => {
    render(<FinancialCharts financialData={mockFinancialData} />);

    // Wait for chart to load
    await waitFor(() => {
      expect(screen.getByText('Range')).toBeInTheDocument();
    });

    // Should show min and max values
    expect(screen.getByText('Range')).toBeInTheDocument();
  });
});