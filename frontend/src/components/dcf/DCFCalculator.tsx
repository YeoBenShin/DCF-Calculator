import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { financialService } from '../../services/financialService';
import { FinancialData, DCFInput } from '../../types';
import { 
  formatToBigDecimal, 
  isValidBigDecimal, 
  parseBigDecimal,
  formatBigDecimalForDisplay 
} from '../../utils/apiUtils';
import './DCFCalculator.css';

interface FormData {
  ticker: string;
  discountRate: string;
  growthRate: string;
  terminalGrowthRate: string;
}

interface FormErrors {
  ticker?: string;
  discountRate?: string;
  growthRate?: string;
  terminalGrowthRate?: string;
  general?: string;
}

const DCFCalculator: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<FormData>({
    ticker: '',
    discountRate: '',
    growthRate: '',
    terminalGrowthRate: ''
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [isLoading, setIsLoading] = useState(false);
  const [financialData, setFinancialData] = useState<FinancialData | null>(null);

  // Helper function to safely format BigDecimal values for display
  const formatFinancialValue = (value: string | number | undefined | null): string => {
    if (!value && value !== 0) return '0';
    try {
      // Convert to string if it's a number (for test compatibility)
      const stringValue = typeof value === 'number' ? value.toString() : value;
      const formatted = formatBigDecimalForDisplay(stringValue, 0);
      return formatted.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    } catch (error) {
      console.warn('Error formatting financial value:', value, error);
      return '0';
    }
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Ticker validation
    if (!formData.ticker.trim()) {
      newErrors.ticker = 'Stock ticker is required';
    } else if (!/^[A-Za-z]{1,5}$/.test(formData.ticker.trim())) {
      newErrors.ticker = 'Please enter a valid stock ticker (1-5 letters)';
    }

    // Discount rate validation with BigDecimal precision
    if (!formData.discountRate.trim()) {
      newErrors.discountRate = 'Discount rate is required';
    } else {
      const discountRateStr = formatToBigDecimal(parseFloat(formData.discountRate), 6);
      if (!isValidBigDecimal(discountRateStr)) {
        newErrors.discountRate = 'Discount rate must be a valid number';
      } else {
        const discountRate = parseBigDecimal(discountRateStr);
        if (discountRate <= 0 || discountRate > 100) {
          newErrors.discountRate = 'Discount rate must be between 0.1% and 100%';
        }
      }
    }

    // Growth rate validation with BigDecimal precision
    if (!formData.growthRate.trim()) {
      newErrors.growthRate = 'Growth rate is required';
    } else {
      const growthRateStr = formatToBigDecimal(parseFloat(formData.growthRate), 6);
      if (!isValidBigDecimal(growthRateStr)) {
        newErrors.growthRate = 'Growth rate must be a valid number';
      } else {
        const growthRate = parseBigDecimal(growthRateStr);
        if (growthRate > 1000) {
          newErrors.growthRate = 'Growth rate too high. Please input a realistic value.';
        } else if (growthRate < -100) {
          newErrors.growthRate = 'Growth rate cannot be less than -100%';
        }
      }
    }

    // Terminal growth rate validation with BigDecimal precision
    if (!formData.terminalGrowthRate.trim()) {
      newErrors.terminalGrowthRate = 'Terminal growth rate is required';
    } else {
      const terminalGrowthRateStr = formatToBigDecimal(parseFloat(formData.terminalGrowthRate), 6);
      if (!isValidBigDecimal(terminalGrowthRateStr)) {
        newErrors.terminalGrowthRate = 'Terminal growth rate must be a valid number';
      } else {
        const terminalGrowthRate = parseBigDecimal(terminalGrowthRateStr);
        if (terminalGrowthRate > 10) {
          newErrors.terminalGrowthRate = 'Terminal growth rate should typically be below 10%';
        } else if (terminalGrowthRate < 0) {
          newErrors.terminalGrowthRate = 'Terminal growth rate cannot be negative';
        }
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

    // Handle numeric inputs with BigDecimal precision formatting
    if (['discountRate', 'growthRate', 'terminalGrowthRate'].includes(name)) {
      // Allow empty string for user input
      if (value === '') {
        processedValue = '';
      } else {
        // Validate numeric input and prevent scientific notation
        const numericValue = parseFloat(value);
        if (!isNaN(numericValue) && isFinite(numericValue)) {
          // Keep the raw input for better user experience during typing
          processedValue = value;
        } else {
          // Don't update if invalid number
          return;
        }
      }
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
      // Create DCF input with proper BigDecimal formatting
      const dcfInput: DCFInput = financialService.createDCFInput(
        formData.ticker.toUpperCase(),
        parseFloat(formData.discountRate),
        parseFloat(formData.growthRate),
        parseFloat(formData.terminalGrowthRate)
      );

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
                    data-testid="ticker-input"
                  />
                  <button
                    type="button"
                    onClick={handleTickerSearch}
                    disabled={isLoading || !formData.ticker.trim()}
                    className="search-button"
                    data-testid="search-button"
                  >
                    {isLoading ? 'Searching...' : 'Search'}
                  </button>
                </div>
                {errors.ticker && <span className="error-message">{errors.ticker}</span>}
              </div>

              {financialData && (
                <div className="financial-data-preview">
                  <h3>✓ Financial data loaded for {financialData.ticker}</h3>
                  <p>Data fetched on: {financialData.date_fetched ? new Date(financialData.date_fetched).toLocaleDateString() : 'Recently'}</p>
                  {financialData.free_cash_flow && financialData.free_cash_flow.length > 0 && (
                    <div className="preview-metrics">
                      <span className="metric">
                        Latest FCF: ${formatFinancialValue(
                          financialData.free_cash_flow[financialData.free_cash_flow.length - 1]
                        )}M
                      </span>
                      {financialData.revenue && financialData.revenue.length > 0 && (
                        <span className="metric">
                          Latest Revenue: ${formatFinancialValue(
                            financialData.revenue[financialData.revenue.length - 1]
                          )}M
                        </span>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* DCF Parameters Section */}
          <div className="form-section">
            <h2>DCF Parameters</h2>
            <div className="parameters-grid">
              <div className="form-group">
                <label htmlFor="discountRate">
                  Discount Rate (%)
                  <span className="help-text">Required rate of return</span>
                </label>
                <input
                  type="number"
                  id="discountRate"
                  name="discountRate"
                  value={formData.discountRate}
                  onChange={handleInputChange}
                  placeholder="e.g., 10.5"
                  step="0.000001"
                  min="0.000001"
                  max="100"
                  className={errors.discountRate ? 'error' : ''}
                  data-testid="discount-rate-input"
                />
                {errors.discountRate && <span className="error-message">{errors.discountRate}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="growthRate">
                  Growth Rate (%)
                  <span className="help-text">Expected annual growth</span>
                </label>
                <input
                  type="number"
                  id="growthRate"
                  name="growthRate"
                  value={formData.growthRate}
                  onChange={handleInputChange}
                  placeholder="e.g., 15.25"
                  step="0.000001"
                  min="-100"
                  max="1000"
                  className={errors.growthRate ? 'error' : ''}
                  data-testid="growth-rate-input"
                />
                {errors.growthRate && <span className="error-message">{errors.growthRate}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="terminalGrowthRate">
                  Terminal Growth Rate (%)
                  <span className="help-text">Long-term growth rate</span>
                </label>
                <input
                  type="number"
                  id="terminalGrowthRate"
                  name="terminalGrowthRate"
                  value={formData.terminalGrowthRate}
                  onChange={handleInputChange}
                  placeholder="e.g., 2.5"
                  step="0.000001"
                  min="0"
                  max="10"
                  className={errors.terminalGrowthRate ? 'error' : ''}
                  data-testid="terminal-growth-rate-input"
                />
                {errors.terminalGrowthRate && <span className="error-message">{errors.terminalGrowthRate}</span>}
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
            data-testid="calculate-dcf-button"
          >
            {isLoading ? 'Calculating...' : 'Calculate Fair Value'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default DCFCalculator;