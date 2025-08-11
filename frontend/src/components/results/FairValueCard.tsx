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

  const calculateUpside = (): number => {
    return ((dcfResult.fair_value_per_share - dcfResult.current_price) / dcfResult.current_price) * 100;
  };

  const upside = calculateUpside();

  return (
    <div className="fair-value-card">
      <div className="card-header">
        <h2>Fair Value Analysis</h2>
        <div className={`valuation-badge ${getValuationColor(dcfResult.valuation)}`}>
          <span className="valuation-icon">{getValuationIcon(dcfResult.valuation)}</span>
          <span className="valuation-text">{dcfResult.valuation}</span>
        </div>
      </div>

      <div className="card-content">
        <div className="price-comparison">
          <div className="price-item">
            <label>Fair Value</label>
            <div className="price-value primary">
              ${dcfResult.fair_value_per_share.toFixed(2)}
            </div>
          </div>
          
          <div className="price-divider">vs</div>
          
          <div className="price-item">
            <label>Current Price</label>
            <div className="price-value secondary">
              ${dcfResult.current_price.toFixed(2)}
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
              <span>{dcfInput.discount_rate}%</span>
            </div>
            <div className="parameter-item">
              <label>Growth Rate</label>
              <span>{dcfInput.growth_rate}%</span>
            </div>
            <div className="parameter-item">
              <label>Terminal Growth Rate</label>
              <span>{dcfInput.terminal_growth_rate}%</span>
            </div>
          </div>
        </div>

        <div className="disclaimer">
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