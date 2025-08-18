import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FinancialCharts from '../FinancialCharts';
import FairValueCard from '../FairValueCard';
import { FinancialData, DCFOutput, DCFInput } from '../../../types';

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

describe('BigDecimal Visualization Components', () => {
  describe('FinancialCharts with extreme BigDecimal values', () => {
    it('handles very large BigDecimal values (trillions)', async () => {
      const largeValueData: FinancialData = {
        ticker: 'MEGA',
        revenue: ['1000000000000000.00', '2000000000000000.00', '3000000000000000.00'], // Quadrillions
        operating_expense: ['800000000000000.00', '1600000000000000.00', '2400000000000000.00'],
        operating_income: ['200000000000000.00', '400000000000000.00', '600000000000000.00'],
        operating_cash_flow: ['250000000000000.00', '500000000000000.00', '750000000000000.00'],
        net_profit: ['150000000000000.00', '300000000000000.00', '450000000000000.00'],
        capital_expenditure: ['50000000000000.00', '100000000000000.00', '150000000000000.00'],
        free_cash_flow: ['200000000000000.00', '400000000000000.00', '600000000000000.00'],
        eps: ['1000.123456', '2000.654321', '3000.987654'], // Large EPS values
        total_debt: ['500000000000000.00', '1000000000000000.00', '1500000000000000.00'],
        ordinary_shares_number: ['1000000000000.00', '1000000000000.00', '1000000000000.00'],
        date_fetched: '2025-01-01'
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

      // Should show summary stats
      expect(screen.getByText('Latest Value')).toBeInTheDocument();
      expect(screen.getByText('Trend')).toBeInTheDocument();
      expect(screen.getByText('Range')).toBeInTheDocument();
    });

    it('handles very small BigDecimal values (micro amounts)', async () => {
      const smallValueData: FinancialData = {
        ticker: 'MICRO',
        revenue: ['0.000001', '0.000002', '0.000003'], // Very small values
        operating_expense: ['0.0000008', '0.0000016', '0.0000024'],
        operating_income: ['0.0000002', '0.0000004', '0.0000006'],
        operating_cash_flow: ['0.00000025', '0.0000005', '0.00000075'],
        net_profit: ['0.00000015', '0.0000003', '0.00000045'],
        capital_expenditure: ['0.00000005', '0.0000001', '0.00000015'],
        free_cash_flow: ['0.0000002', '0.0000004', '0.0000006'],
        eps: ['0.000123', '0.000456', '0.000789'], // Very small EPS values
        total_debt: ['0.0000005', '0.000001', '0.0000015'],
        ordinary_shares_number: ['1000000.00', '1000000.00', '1000000.00'],
        date_fetched: '2025-01-01'
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

    it('handles scientific notation BigDecimal strings', async () => {
      const scientificData: FinancialData = {
        ticker: 'SCI',
        revenue: ['1.23E+12', '2.45E+12', '3.67E+12'], // Scientific notation
        operating_expense: ['9.87E+11', '1.96E+12', '2.94E+12'],
        operating_income: ['2.43E+11', '4.9E+11', '7.3E+11'],
        operating_cash_flow: ['3.0E+11', '6.1E+11', '9.1E+11'],
        net_profit: ['1.8E+11', '3.7E+11', '5.5E+11'],
        capital_expenditure: ['6.0E+10', '1.2E+11', '1.8E+11'],
        free_cash_flow: ['2.4E+11', '4.9E+11', '7.3E+11'],
        eps: ['1.23E+2', '2.45E+2', '3.67E+2'], // Scientific EPS
        total_debt: ['6.0E+11', '1.2E+12', '1.8E+12'],
        ordinary_shares_number: ['1.0E+9', '1.0E+9', '1.0E+9'],
        date_fetched: '2025-01-01'
      };

      render(<FinancialCharts financialData={scientificData} />);

      // Should parse scientific notation correctly
      expect(screen.getByText('Financial Performance')).toBeInTheDocument();
      expect(screen.getByText('Revenue Trend')).toBeInTheDocument();
    });

    it('handles mixed precision BigDecimal values', async () => {
      const mixedPrecisionData: FinancialData = {
        ticker: 'MIX',
        revenue: ['123456789.123456789', '234567890.987654321', '345678901.555555555'],
        operating_expense: ['98765432.111111111', '187654321.222222222', '276543210.333333333'],
        operating_income: ['24691357.012345678', '46913569.765432109', '69135691.222222222'],
        operating_cash_flow: ['30864196.265432109', '58641975.956790123', '86419753.777777777'],
        net_profit: ['18518518.159753951', '35185185.574074074', '51851852.444444444'],
        capital_expenditure: ['6172839.053251953', '11728395.191358025', '17283951.148148148'],
        free_cash_flow: ['24691357.212202156', '46913569.765432109', '69135691.629629629'],
        eps: ['12.345678901234', '23.456789012345', '34.567890123456'],
        total_debt: ['61728395.321987654', '117283950.643975309', '172839506.965962963'],
        ordinary_shares_number: ['1500000.000000000', '1500000.000000000', '1500000.000000000'],
        date_fetched: '2025-01-01'
      };

      render(<FinancialCharts financialData={mixedPrecisionData} />);

      expect(screen.getByText('Financial Performance')).toBeInTheDocument();
      expect(screen.getByText('Revenue Trend')).toBeInTheDocument();
    });
  });

  describe('FairValueCard with extreme BigDecimal values', () => {
    const mockDCFInput: DCFInput = {
      ticker: 'TEST',
      discountRate: '10.123456',
      growthRate: '15.987654',
      terminalGrowthRate: '2.555555'
    };

    it('handles very large fair value amounts', () => {
      const largeFairValueResult: DCFOutput = {
        ticker: 'MEGA',
        fairValuePerShare: '1234567890.123456',
        currentPrice: '987654321.987654',
        valuation: 'Undervalued'
      };

      render(<FairValueCard dcfResult={largeFairValueResult} dcfInput={mockDCFInput} />);

      expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
      expect(screen.getByText('Undervalued')).toBeInTheDocument();
      
      // Should format large values appropriately
      expect(screen.getByText('$1234.57M')).toBeInTheDocument();
      expect(screen.getByText('$987.65M')).toBeInTheDocument();
    });

    it('handles very small fair value amounts', () => {
      const smallFairValueResult: DCFOutput = {
        ticker: 'MICRO',
        fairValuePerShare: '0.000123456',
        currentPrice: '0.000098765',
        valuation: 'Undervalued'
      };

      render(<FairValueCard dcfResult={smallFairValueResult} dcfInput={mockDCFInput} />);

      expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
      expect(screen.getByText('Undervalued')).toBeInTheDocument();
      
      // Should format small values with appropriate precision
      expect(screen.getByText('$0.000123')).toBeInTheDocument();
      expect(screen.getByText('$0.000099')).toBeInTheDocument();
    });

    it('handles high precision BigDecimal parameters', () => {
      const highPrecisionInput: DCFInput = {
        ticker: 'PRECISE',
        discountRate: '10.123456789',
        growthRate: '15.987654321',
        terminalGrowthRate: '2.555555555'
      };

      const dcfResult: DCFOutput = {
        ticker: 'PRECISE',
        fairValuePerShare: '175.123456789',
        currentPrice: '150.987654321',
        valuation: 'Undervalued'
      };

      render(<FairValueCard dcfResult={dcfResult} dcfInput={highPrecisionInput} />);

      // Should display parameters with 2 decimal places
      expect(screen.getByText('10.12%')).toBeInTheDocument();
      expect(screen.getByText('15.99%')).toBeInTheDocument();
      expect(screen.getByText('2.56%')).toBeInTheDocument();
    });

    it('handles zero and near-zero BigDecimal values', () => {
      const zeroValueResult: DCFOutput = {
        ticker: 'ZERO',
        fairValuePerShare: '0.000000001',
        currentPrice: '0.000000002',
        valuation: 'Overvalued'
      };

      render(<FairValueCard dcfResult={zeroValueResult} dcfInput={mockDCFInput} />);

      expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
      expect(screen.getByText('Overvalued')).toBeInTheDocument();
      
      // Should handle near-zero values gracefully
      expect(screen.getAllByText('$0.00')).toHaveLength(2); // Fair value and current price
    });
  });

  describe('Error handling for invalid BigDecimal values', () => {
    it('handles invalid BigDecimal strings in charts', () => {
      const invalidData: FinancialData = {
        ticker: 'INVALID',
        revenue: ['invalid', 'NaN', ''], // Invalid BigDecimal strings
        operating_expense: ['not-a-number', '', 'undefined'],
        operating_income: ['null', 'infinity', '-infinity'],
        operating_cash_flow: ['abc', '123abc', 'xyz789'],
        net_profit: ['', '   ', '\t\n'],
        capital_expenditure: ['1.2.3', '1,000', '1 000'],
        free_cash_flow: ['1e', 'e10', '1.e'],
        eps: ['not-a-number', '', 'undefined'],
        total_debt: ['invalid', 'NaN', ''],
        ordinary_shares_number: ['invalid', 'NaN', ''],
        date_fetched: '2025-01-01'
      };

      render(<FinancialCharts financialData={invalidData} />);

      // Should render without errors and show no data message
      expect(screen.getByText('No data available for Revenue Trend')).toBeInTheDocument();
    });

    it('handles invalid BigDecimal strings in fair value card', () => {
      const invalidResult: DCFOutput = {
        ticker: 'INVALID',
        fairValuePerShare: 'not-a-number',
        currentPrice: 'invalid',
        valuation: 'Undervalued'
      };

      const invalidInput: DCFInput = {
        ticker: 'INVALID',
        discountRate: 'invalid',
        growthRate: 'not-a-number',
        terminalGrowthRate: 'abc'
      };

      render(<FairValueCard dcfResult={invalidResult} dcfInput={invalidInput} />);

      // Should handle invalid values gracefully
      expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
      expect(screen.getAllByText('$0.00')).toHaveLength(2); // Should default to 0 for both values
    });
  });
});