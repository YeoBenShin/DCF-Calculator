import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { financialService } from '../../services/financialService';
import { FinancialData, DCFInput } from '../../types';
import './DCFCalculator.css';

interface FormData {
  ticker: string;
  discount_rate: string;
  growth_rate: string;
  terminal_growth_rate: string;
}

interface FormErrors {
  ticker?: string;
  discount_rate?: string;
  growth_rate?: string;
  terminal_growth_rate?: string;
  general?: string;
}

const DCFCalculator: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<FormData>({
    ticker: '',
    discount_rate: '',
    growth_rate: '',
    terminal_growth_rate: ''
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [isLoading, setIsLoading] = useState(false);
  const [financialData, setFinancialData] = useState<FinancialData | null>(null);

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Ticker validation
    if (!formData.ticker.trim()) {
      newErrors.ticker = 'Stock ticker is required';
    } else if (!/^[A-Za-z]{1,5}$/.test(formData.ticker.trim())) {
      newErrors.ticker = 'Please enter a valid stock ticker (1-5 letters)';
    }

    // Discount rate validation
    if (!formData.discount_rate.trim()) {
      newErrors.discount_rate = 'Discount rate is required';
    } else {
      const discountRate = parseFloat(formData.discount_rate);
      if (isNaN(discountRate) || discountRate <= 0 || discountRate > 100) {
        newErrors.discount_rate = 'Discount rate must be between 0.1% and 100%';
      }
    }

    // Growth rate validation
    if (!formData.growth_rate.trim()) {
      newErrors.growth_rate = 'Growth rate is required';
    } else {
      const growthRate = parseFloat(formData.growth_rate);
      if (isNaN(growthRate)) {
        newErrors.growth_rate = 'Growth rate must be a valid number';
      } else if (growthRate > 1000) {
        newErrors.growth_rate = 'Growth rate too high. Please input a realistic value.';
      } else if (growthRate < -100) {
        newErrors.growth_rate = 'Growth rate cannot be less than -100%';
      }
    }

    // Terminal growth rate validation
    if (!formData.terminal_growth_rate.trim()) {
      newErrors.terminal_growth_rate = 'Terminal growth rate is required';
    } else {
      const terminalGrowthRate = parseFloat(formData.terminal_growth_rate);
      if (isNaN(terminalGrowthRate)) {
        newErrors.terminal_growth_rate = 'Terminal growth rate must be a valid number';
      } else if (terminalGrowthRate > 10) {
        newErrors.terminal_growth_rate = 'Terminal growth rate should typically be below 10%';
      } else if (terminalGrowthRate < 0) {
        newErrors.terminal_growth_rate = 'Terminal growth rate cannot be negative';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let processedValue = value;
    
    // Handle ticker input special formatting
    if (name === 'ticker') {
      processedValue = value.toUpperCase().slice(0, 5);
    }
    
    setFormData(prev => ({
      ...prev,
      [name]: processedValue
    }));

    // Clear error for this field when user starts typing
    if (errors[name as keyof FormErrors]) {
      setErrors(prev => ({
        ...prev,
        [name]: undefined
      }));
    }
  };

  const handleTickerSearch = async () => {
    if (!formData.ticker.trim()) {
      setErrors(prev => ({ ...prev, ticker: 'Please enter a stock ticker' }));
      return;
    }

    setIsLoading(true);
    setErrors(prev => ({ ...prev, general: undefined }));

    try {
      const data = await financialService.getFinancialData(formData.ticker);
      setFinancialData(data);
      setErrors(prev => ({ ...prev, ticker: undefined }));
    } catch (error: any) {
      setErrors(prev => ({ ...prev, general: error.message }));
      setFinancialData(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    if (!financialData) {
      setErrors(prev => ({ ...prev, general: 'Please search for financial data first' }));
      return;
    }

    setIsLoading(true);
    setErrors(prev => ({ ...prev, general: undefined }));

    try {
      const dcfInput: DCFInput = {
        ticker: formData.ticker.toUpperCase(),
        discount_rate: parseFloat(formData.discount_rate),
        growth_rate: parseFloat(formData.growth_rate),
        terminal_growth_rate: parseFloat(formData.terminal_growth_rate)
      };

      const result = await financialService.calculateDCF(dcfInput);
      
      // Navigate to results page with the calculation result and financial data
      navigate('/results', { 
        state: { 
          dcfResult: result, 
          financialData: financialData,
          dcfInput: dcfInput
        } 
      });
    } catch (error: any) {
      setErrors(prev => ({ ...prev, general: error.message }));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="dcf-calculator">
      <div className="dcf-calculator-container">
        <h1>DCF Calculator</h1>
        <p className="subtitle">Calculate the intrinsic value of stocks using the Discounted Cash Flow model</p>

        <form onSubmit={handleSubmit} className="dcf-form">
          {/* Stock Ticker Search Section */}
          <div className="form-section">
            <h2>Stock Information</h2>
            <div className="ticker-search">
              <div className="form-group">
                <label htmlFor="ticker">Stock Ticker Symbol</label>
                <div className="ticker-input-group">
                  <input
                    type="text"
                    id="ticker"
                    name="ticker"
                    value={formData.ticker}
                    onChange={handleInputChange}
                    placeholder="e.g., AAPL"
                    className={errors.ticker ? 'error' : ''}
                  />
                  <button
                    type="button"
                    onClick={handleTickerSearch}
                    disabled={isLoading || !formData.ticker.trim()}
                    className="search-button"
                  >
                    {isLoading ? 'Searching...' : 'Search'}
                  </button>
                </div>
                {errors.ticker && <span className="error-message">{errors.ticker}</span>}
              </div>

              {financialData && (
                <div className="financial-data-preview">
                  <h3>✓ Financial data loaded for {financialData.ticker}</h3>
                  <p>Data fetched on: {new Date(financialData.date_fetched).toLocaleDateString()}</p>
                </div>
              )}
            </div>
          </div>

          {/* DCF Parameters Section */}
          <div className="form-section">
            <h2>DCF Parameters</h2>
            <div className="parameters-grid">
              <div className="form-group">
                <label htmlFor="discount_rate">
                  Discount Rate (%)
                  <span className="help-text">Required rate of return</span>
                </label>
                <input
                  type="number"
                  id="discount_rate"
                  name="discount_rate"
                  value={formData.discount_rate}
                  onChange={handleInputChange}
                  placeholder="e.g., 10"
                  step="0.1"
                  min="0.1"
                  max="100"
                  className={errors.discount_rate ? 'error' : ''}
                />
                {errors.discount_rate && <span className="error-message">{errors.discount_rate}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="growth_rate">
                  Growth Rate (%)
                  <span className="help-text">Expected annual growth</span>
                </label>
                <input
                  type="number"
                  id="growth_rate"
                  name="growth_rate"
                  value={formData.growth_rate}
                  onChange={handleInputChange}
                  placeholder="e.g., 15"
                  step="0.1"
                  className={errors.growth_rate ? 'error' : ''}
                />
                {errors.growth_rate && <span className="error-message">{errors.growth_rate}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="terminal_growth_rate">
                  Terminal Growth Rate (%)
                  <span className="help-text">Long-term growth rate</span>
                </label>
                <input
                  type="number"
                  id="terminal_growth_rate"
                  name="terminal_growth_rate"
                  value={formData.terminal_growth_rate}
                  onChange={handleInputChange}
                  placeholder="e.g., 2.5"
                  step="0.1"
                  min="0"
                  max="10"
                  className={errors.terminal_growth_rate ? 'error' : ''}
                />
                {errors.terminal_growth_rate && <span className="error-message">{errors.terminal_growth_rate}</span>}
              </div>
            </div>
          </div>

          {/* Error Display */}
          {errors.general && (
            <div className="error-banner">
              {errors.general}
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading || !financialData}
            className="calculate-button"
          >
            {isLoading ? 'Calculating...' : 'Calculate Fair Value'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default DCFCalculator;