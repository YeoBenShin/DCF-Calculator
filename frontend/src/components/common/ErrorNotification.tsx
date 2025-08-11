import React, { useEffect } from 'react';
import { useApp } from '../../contexts/AppContext';
import './ErrorNotification.css';

const ErrorNotification: React.FC = () => {
  const { state, clearError } = useApp();

  useEffect(() => {
    if (state.error) {
      // Auto-clear error after 5 seconds
      const timer = setTimeout(() => {
        clearError();
      }, 5000);

      return () => clearTimeout(timer);
    }
  }, [state.error, clearError]);

  if (!state.error) {
    return null;
  }

  return (
    <div className="error-notification">
      <div className="error-notification-content">
        <div className="error-icon">⚠️</div>
        <div className="error-message">{state.error}</div>
        <button 
          className="error-close-button"
          onClick={clearError}
          aria-label="Close error notification"
        >
          ×
        </button>
      </div>
    </div>
  );
};

export default ErrorNotification;