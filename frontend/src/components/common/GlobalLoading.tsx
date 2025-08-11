import React from 'react';
import { useApp } from '../../contexts/AppContext';
import './GlobalLoading.css';

const GlobalLoading: React.FC = () => {
  const { state } = useApp();

  if (!state.isLoading) {
    return null;
  }

  return (
    <div className="global-loading-overlay">
      <div className="global-loading-content">
        <div className="loading-spinner"></div>
        <p className="loading-message">
          {state.globalLoadingMessage || 'Loading...'}
        </p>
      </div>
    </div>
  );
};

export default GlobalLoading;