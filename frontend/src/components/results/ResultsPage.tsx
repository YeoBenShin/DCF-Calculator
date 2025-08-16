import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { DCFOutput, FinancialData, DCFInput } from '../../types';
import { watchlistService } from '../../services/watchlistService';
import { useApp } from '../../contexts/AppContext';
import FairValueCard from './FairValueCard';
import FinancialCharts from './FinancialCharts';
import './ResultsPage.css';

interface LocationState {
  dcfResult: DCFOutput;
  financialData: FinancialData;
  dcfInput: DCFInput;
} 

const ResultsPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { setError } = useApp();
  const state = location.state as LocationState;
  const [isAddingToWatchlist, setIsAddingToWatchlist] = useState(false);
  const [addedToWatchlist, setAddedToWatchlist] = useState(false);

  // Redirect to calculator if no data is available
  React.useEffect(() => {
    if (!state || !state.dcfResult || !state.financialData) {
      navigate('/calculator');
    }
  }, [state, navigate]);

  if (!state || !state.dcfResult || !state.financialData) {
    return (
      <div className="results-page">
        <div className="loading-container">
          <div className="loading-spinner" data-testid="loading-spinner"></div>
          <p>Redirecting to calculator...</p>
        </div>
      </div>
    );
  }

  const { dcfResult, financialData, dcfInput } = state;

  const handleNewCalculation = () => {
    navigate('/calculator');
  };

  const handleAddToWatchlist = async () => {
    if (!state?.dcfResult?.ticker) {
      setError('No ticker available to add to watchlist');
      return;
    }

    setIsAddingToWatchlist(true);
    
    try {
      await watchlistService.addToWatchlist(state.dcfResult.ticker);
      setAddedToWatchlist(true);
    } catch (error: any) {
      setError(error.message || 'Failed to add to watchlist');
    } finally {
      setIsAddingToWatchlist(false);
    }
  };

  return (
    <div className="results-page">
      <div className="results-container">
        <div className="results-header">
          <h1>DCF Analysis Results</h1>
          <p className="ticker-name">{dcfResult.ticker}</p>
        </div>

        <div className="results-content">
          {/* Fair Value Results Card */}
          <div className="results-section">
            <FairValueCard 
              dcfResult={dcfResult}
              dcfInput={dcfInput}
            />
          </div>

          {/* Financial Charts */}
          <div className="results-section">
            <FinancialCharts 
              financialData={financialData}
            />
          </div>

          {/* Action Buttons */}
          <div className="results-actions">
            <button 
              onClick={handleNewCalculation}
              className="action-button primary"
            >
              New Calculation
            </button>
            <button 
              onClick={handleAddToWatchlist}
              className="action-button secondary"
              disabled={isAddingToWatchlist || addedToWatchlist}
              data-testid="add-to-watchlist-button"
            >
              {isAddingToWatchlist 
                ? 'Adding...' 
                : addedToWatchlist 
                  ? 'Added to Watchlist' 
                  : 'Add to Watchlist'
              }
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResultsPage;