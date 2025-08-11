import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useApp } from '../../contexts/AppContext';
import { authService } from '../../services/authService';
import './Navigation.css';

const Navigation: React.FC = () => {
  const { state, logout } = useApp();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    authService.logout();
    logout();
    navigate('/login');
  };

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  return (
    <nav className="navigation">
      <div className="nav-container">
        <Link to="/" className="nav-brand">
          DCF Calculator
        </Link>
        
        <div className="nav-links">
          {state.isAuthenticated ? (
            <>
              <Link 
                to="/calculator" 
                className={`nav-link ${isActive('/calculator') ? 'active' : ''}`}
              >
                Calculator
              </Link>
              <Link 
                to="/watchlist" 
                className={`nav-link ${isActive('/watchlist') ? 'active' : ''}`}
              >
                Watchlist
              </Link>
              <div className="nav-user-info">
                <span className="user-email">{state.user?.email}</span>
                <button onClick={handleLogout} className="logout-button">
                  Logout
                </button>
              </div>
            </>
          ) : (
            <>
              <Link 
                to="/login" 
                className={`nav-link ${isActive('/login') ? 'active' : ''}`}
              >
                Login
              </Link>
              <Link 
                to="/signup" 
                className={`nav-link ${isActive('/signup') ? 'active' : ''}`}
              >
                Sign Up
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navigation;