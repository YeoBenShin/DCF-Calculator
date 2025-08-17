import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AppProvider, useApp } from './contexts/AppContext';
import { authService, setGlobalHandlers } from './services/authService';
import ErrorBoundary from './components/common/ErrorBoundary';
import GlobalLoading from './components/common/GlobalLoading';
import ErrorNotification from './components/common/ErrorNotification';
import Navigation from './components/common/Navigation';
import ProtectedRoute from './components/common/ProtectedRoute';
import Login from './components/auth/Login';
import Signup from './components/auth/Signup';
import DCFCalculator from './components/dcf/DCFCalculator';
import ResultsPage from './components/results/ResultsPage';
import Watchlist from './components/watchlist/Watchlist';
import './App.css';

// Placeholder components - will be implemented in later tasks
const LandingPage = () => (
  <div className="landing-page">
    <div className="landing-content">
      <h1>DCF Calculator</h1>
      <p>Calculate the intrinsic value of stocks using the Discounted Cash Flow model</p>
      <div className="landing-actions">
        <a href="/calculator" className="btn btn-primary">Get Started</a>
        <a href="/login" className="btn btn-secondary">Login</a>
      </div>
    </div>
  </div>
);

// Main App component that uses the context
const AppContent: React.FC = () => {
  const { setUser, setLoading, setError } = useApp();

  useEffect(() => {
    // Set up global handlers for API services
    setGlobalHandlers(setError, setLoading);

    // Check if user is already authenticated on app load
    const initializeAuth = async () => {
      setLoading(true, 'Initializing application...');
      
      try {
        const token = authService.getToken();
        const user = authService.getCurrentUser();
        
        // Validate that we have both token and a proper user object
        if (token && user && typeof user === 'object' && user.email) {
          setUser(user);
        } else if (token || user) {
          // If we have partial data, clear it to avoid inconsistent state
          console.warn('Inconsistent auth state detected, clearing...');
          authService.logout();
        }
      } catch (error) {
        console.error('Error initializing auth:', error);
        // Clear potentially corrupted data
        authService.logout();
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, [setUser, setLoading, setError]);

  return (
    <Router>
      <div className="App">
        <Navigation />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route 
              path="/calculator" 
              element={
                <ProtectedRoute>
                  <DCFCalculator />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/results" 
              element={
                <ProtectedRoute>
                  <ResultsPage />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/watchlist" 
              element={
                <ProtectedRoute>
                  <Watchlist />
                </ProtectedRoute>
              } 
            />
            {/* Redirect unknown routes to home */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <GlobalLoading />
        <ErrorNotification />
      </div>
    </Router>
  );
};

// Root App component with providers
function App() {
  return (
    <ErrorBoundary>
      <AppProvider>
        <AppContent />
      </AppProvider>
    </ErrorBoundary>
  );
}

export default App;