import React from 'react';
import { DCFOutput, DCFInput } from '../../types';
import './FairValueCard.css';

interface FairValueCardProps {
  dcfResult: DCFOutput;
  dcfInput: DCFInput;
}

const FairValueCard: React.FC<FairValueCardProps> = ({ dcfResult, dcfInput }) => {

  const getValuationColor = (valuation: string): string => {
    switch (valuation) {
      case 'Undervalued':
        return 'green';
      case 'Overvalued':
        return 'red';
      case 'Fair Value':
        return 'blue';
      default:
        return 'gray';
    }
  };

  const getValuationIcon = (valuation: string): string => {
    switch (valuation) {
      case 'Undervalued':
        return '📈';
      case 'Overvalued':
        return '📉';
      case 'Fair Value':
        return '⚖️';
      default:
        return '❓';
    }
  };

  // Safely convert BigDecimal string values to numbers
  const fairValue = typeof dcfResult.fairValuePerShare === 'number'
    ? dcfResult.fairValuePerShare
    : parseFloat(dcfResult.fairValuePerShare as any) || 0;

  const currentPrice = typeof dcfResult.currentPrice === 'number'
    ? dcfResult.currentPrice
    : parseFloat(dcfResult.currentPrice as any) || 0;

  // Enhanced formatting for BigDecimal values
  const formatPrice = (value: number): string => {
    if (Math.abs(value) < 1e-6) {
      return '$0.00';
    } else if (Math.abs(value) < 0.01) {
      return `$${value.toFixed(6)}`; // Very small values
    } else if (Math.abs(value) < 1) {
      return `$${value.toFixed(4)}`; // Small values
    } else if (Math.abs(value) < 1000) {
      return `$${value.toFixed(2)}`; // Normal values
    } else if (Math.abs(value) >= 1e6) {
      return `$${(value / 1e6).toFixed(2)}M`; // Millions
    } else if (Math.abs(value) >= 1e3) {
      return `$${(value / 1e3).toFixed(2)}K`; // Thousands
    } else {
      return `$${value.toFixed(2)}`;
    }
  };

  const calculateUpside = (): number => {
    if (currentPrice === 0) return 0;
    return ((fairValue - currentPrice) / currentPrice) * 100;
  };

  const upside = calculateUpside();

  return (
    <div className="fair-value-card" data-testid="fair-value-card">
      <div className="card-header">
        <h2>Fair Value Analysis</h2>
        <div className={`valuation-badge ${getValuationColor(dcfResult.valuation)} ${dcfResult.valuation.toLowerCase()}`}>
          <span className="valuation-icon">{getValuationIcon(dcfResult.valuation)}</span>
          <span className="valuation-text">{dcfResult.valuation}</span>
        </div>
      </div>

      <div className="card-content">
        <div className="price-comparison">
          <div className="price-item">
            <label>Fair Value</label>
            <div className="price-value primary">
              {formatPrice(fairValue)}
            </div>
          </div>

          <div className="price-divider">vs</div>

          <div className="price-item">
            <label>Current Price</label>
            <div className="price-value secondary">
              {formatPrice(currentPrice)}
            </div>
          </div>
        </div>

        <div className="upside-section">
          <div className="upside-label">Potential Upside/Downside</div>
          <div className={`upside-value ${upside >= 0 ? 'positive' : 'negative'}`}>
            {upside >= 0 ? '+' : ''}{upside.toFixed(1)}%
          </div>
        </div>

        <div className="dcf-parameters">
          <h3>DCF Parameters Used</h3>
          <div className="parameters-grid">
            <div className="parameter-item">
              <label>Discount Rate</label>
              <span>{(parseFloat(dcfInput.discountRate) || 0).toFixed(2)}%</span>
            </div>
            <div className="parameter-item">
              <label>Growth Rate</label>
              <span>{(parseFloat(dcfInput.growthRate) || 0).toFixed(2)}%</span>
            </div>
            <div className="parameter-item">
              <label>Terminal Growth Rate</label>
              <span>{(parseFloat(dcfInput.terminalGrowthRate) || 0).toFixed(2)}%</span>
            </div>
          </div>
        </div>

        <div className="disclaimer">
          <p>
            <strong>Data Source:</strong> Financial data is sourced from publicly available information and may include estimated values for demonstration purposes.
          </p>
          <p>
            <strong>Disclaimer:</strong> This analysis is for educational purposes only and should not be considered as investment advice.
            Please conduct your own research and consult with a financial advisor before making investment decisions.
          </p>
        </div>
      </div>
    </div>
  );
};

export default FairValueCard;