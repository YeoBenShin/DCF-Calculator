import React from 'react';
import { render, screen } from '@testing-library/react';
import FairValueCard from '../FairValueCard';
import { DCFOutput, DCFInput } from '../../../types';

// Mock data
const mockDCFInput: DCFInput = {
  ticker: 'AAPL',
  discount_rate: 10,
  growth_rate: 15,
  terminal_growth_rate: 2.5
};

describe('FairValueCard', () => {
  it('renders undervalued stock correctly', () => {
    const undervaluedResult: DCFOutput = {
      ticker: 'AAPL',
      fair_value_per_share: 175.50,
      current_price: 150.00,
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
    expect(screen.getByText('10%')).toBeInTheDocument();
    expect(screen.getByText('15%')).toBeInTheDocument();
    expect(screen.getByText('2.5%')).toBeInTheDocument();
  });

  it('renders overvalued stock correctly', () => {
    const overvaluedResult: DCFOutput = {
      ticker: 'TSLA',
      fair_value_per_share: 200.00,
      current_price: 250.00,
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
      fair_value_per_share: 300.00,
      current_price: 300.00,
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
      fair_value_per_share: 175.50,
      current_price: 150.00,
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={undervaluedResult} dcfInput={mockDCFInput} />);

    // Check for undervalued icon (📈)
    expect(screen.getByText('📈')).toBeInTheDocument();
  });

  it('displays DCF parameters correctly', () => {
    const dcfResult: DCFOutput = {
      ticker: 'AAPL',
      fair_value_per_share: 175.50,
      current_price: 150.00,
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
      fair_value_per_share: 175.50,
      current_price: 150.00,
      valuation: 'Undervalued'
    };

    render(<FairValueCard dcfResult={dcfResult} dcfInput={mockDCFInput} />);

    expect(screen.getByText('Disclaimer:')).toBeInTheDocument();
    expect(screen.getByText(/This analysis is for educational purposes only/)).toBeInTheDocument();
  });

  it('handles decimal precision correctly', () => {
    const dcfResult: DCFOutput = {
      ticker: 'AAPL',
      fair_value_per_share: 175.567,
      current_price: 150.123,
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
      fair_value_per_share: 120.00,
      current_price: 100.00,
      valuation: 'Undervalued'
    };

    const { rerender } = render(<FairValueCard dcfResult={positiveUpsideResult} dcfInput={mockDCFInput} />);
    expect(screen.getByText('+20.0%')).toBeInTheDocument();

    // Test negative upside (downside)
    const negativeUpsideResult: DCFOutput = {
      ticker: 'AAPL',
      fair_value_per_share: 80.00,
      current_price: 100.00,
      valuation: 'Overvalued'
    };

    rerender(<FairValueCard dcfResult={negativeUpsideResult} dcfInput={mockDCFInput} />);
    expect(screen.getByText('-20.0%')).toBeInTheDocument();
  });
});