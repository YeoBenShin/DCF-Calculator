import React from 'react';
import { render, screen } from '@testing-library/react';
import FairValueCard from '../FairValueCard';
import { DCFOutput, DCFInput } from '../../../types';

// Mock data - BigDecimal values as strings
const mockDCFInput: DCFInput = {
  ticker: 'AAPL',
  discountRate: '10.00',
  growthRate: '15.00',
  terminalGrowthRate: '2.50'
};

describe('FairValueCard', () => {
  it('renders undervalued stock correctly', () => {
    const undervaluedResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.50',
      currentPrice: '150.00',
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={undervaluedResult} dcfInput={mockDCFInput} />);

    // Check header
    expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
    expect(screen.getByText('Undervalued')).toBeInTheDocument();

    // Check prices
    expect(screen.getByText('$175.50')).toBeInTheDocument();
    expect(screen.getByText('$150.00')).toBeInTheDocument();

    // Check upside calculation (175.50 - 150.00) / 150.00 * 100 = 17.0%
    expect(screen.getByText('+17.0%')).toBeInTheDocument();

    // Check DCF parameters
    expect(screen.getByText('10.00%')).toBeInTheDocument();
    expect(screen.getByText('15.00%')).toBeInTheDocument();
    expect(screen.getByText('2.50%')).toBeInTheDocument();
  });

  it('renders overvalued stock correctly', () => {
    const overvaluedResult: DCFOutput = {
      ticker: 'TSLA',
      fairValuePerShare: '200.00',
      currentPrice: '250.00',
      valuation: 'Overvalued'
    };

    render(<FairValueCard dcfResult={overvaluedResult} dcfInput={mockDCFInput} />);

    expect(screen.getByText('Overvalued')).toBeInTheDocument();
    expect(screen.getByText('$200.00')).toBeInTheDocument();
    expect(screen.getByText('$250.00')).toBeInTheDocument();

    // Check downside calculation (200.00 - 250.00) / 250.00 * 100 = -20.0%
    expect(screen.getByText('-20.0%')).toBeInTheDocument();
  });

  it('renders fair value stock correctly', () => {
    const fairValueResult: DCFOutput = {
      ticker: 'MSFT',
      fairValuePerShare: '300.00',
      currentPrice: '300.00',
      valuation: 'Fair Value'
    };

    render(<FairValueCard dcfResult={fairValueResult} dcfInput={mockDCFInput} />);

    // Use more specific selector to avoid duplicate text issue
    expect(screen.getByText('Fair Value Analysis')).toBeInTheDocument();
    expect(screen.getAllByText('$300.00')).toHaveLength(2); // Fair value and current price
    expect(screen.getByText('⚖️')).toBeInTheDocument(); // Fair value icon

    // Check no upside/downside (0.0%)
    expect(screen.getByText('+0.0%')).toBeInTheDocument();
  });

  it('displays correct valuation icons', () => {
    const undervaluedResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.50',
      currentPrice: '150.00',
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={undervaluedResult} dcfInput={mockDCFInput} />);

    // Check for undervalued icon (📈)
    expect(screen.getByText('📈')).toBeInTheDocument();
  });

  it('displays DCF parameters correctly', () => {
    const dcfResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.50',
      currentPrice: '150.00',
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={dcfResult} dcfInput={mockDCFInput} />);

    expect(screen.getByText('DCF Parameters Used')).toBeInTheDocument();
    expect(screen.getByText('Discount Rate')).toBeInTheDocument();
    expect(screen.getByText('Growth Rate')).toBeInTheDocument();
    expect(screen.getByText('Terminal Growth Rate')).toBeInTheDocument();
  });

  it('displays disclaimer', () => {
    const dcfResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.50',
      currentPrice: '150.00',
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={dcfResult} dcfInput={mockDCFInput} />);

    expect(screen.getByText('Disclaimer:')).toBeInTheDocument();
    expect(screen.getByText(/This analysis is for educational purposes only/)).toBeInTheDocument();
  });

  it('handles decimal precision correctly', () => {
    const dcfResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '175.567',
      currentPrice: '150.123',
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={dcfResult} dcfInput={mockDCFInput} />);

    // Should round to 2 decimal places
    expect(screen.getByText('$175.57')).toBeInTheDocument();
    expect(screen.getByText('$150.12')).toBeInTheDocument();
  });

  it('calculates upside percentage correctly for various scenarios', () => {
    // Test positive upside
    const positiveUpsideResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '120.00',
      currentPrice: '100.00',
      valuation: 'Undervalued'
    };

    const { rerender } = render(<FairValueCard dcfResult={positiveUpsideResult} dcfInput={mockDCFInput} />);
    expect(screen.getByText('+20.0%')).toBeInTheDocument();

    // Test negative upside (downside)
    const negativeUpsideResult: DCFOutput = {
      ticker: 'AAPL',
      fairValuePerShare: '80.00',
      currentPrice: '100.00',
      valuation: 'Overvalued'
    };

    rerender(<FairValueCard dcfResult={negativeUpsideResult} dcfInput={mockDCFInput} />);
    expect(screen.getByText('-20.0%')).toBeInTheDocument();
  });
});