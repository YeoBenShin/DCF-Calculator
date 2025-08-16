import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { AuthRequest, AuthResponse } from '../../types';
import { authService } from '../../services/authService';
import { useApp } from '../../contexts/AppContext';
import './Auth.css';

const Login: React.FC = () => {
  const [formData, setFormData] = useState<AuthRequest>({
    email: '',
    password: ''
  });
  const [errors, setErrors] = useState<Partial<AuthRequest>>({});
  const [apiError, setApiError] = useState<string>('');
  
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser, setLoading } = useApp();
  
  // Get the intended destination from location state
  const from = location.state?.from?.pathname || '/calculator';

  const validateForm = (): boolean => {
    const newErrors: Partial<AuthRequest> = {};
    
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Email is invalid';
    }
    
    if (!formData.password) {
      newErrors.password = 'Password is required';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear field error when user starts typing
    if (errors[name as keyof AuthRequest]) {
      setErrors(prev => ({
        ...prev,
        [name]: undefined
      }));
    }
    
    // Clear API error when user makes changes
    if (apiError) {
      setApiError('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true, 'Logging in...');
    setApiError('');
    
    try {
      const response: AuthResponse = await authService.login(formData);
      // Store token in localStorage
      localStorage.setItem('authToken', response.token);
      localStorage.setItem('user', JSON.stringify(response.user.userId));
      // Update global state
      setUser(response.user);
      
      // Navigate to intended destination or calculator page
      navigate(from, { replace: true });
    } catch (error: any) {
      setApiError(error.message || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Login</h2>
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange}
              className={errors.email ? 'error' : ''}
              placeholder="Enter your email"
            />
            {errors.email && <span className="error-message">{errors.email}</span>}
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className={errors.password ? 'error' : ''}
              placeholder="Enter your password"
            />
            {errors.password && <span className="error-message">{errors.password}</span>}
          </div>
          
          {apiError && <div className="api-error">{apiError}</div>}
          
          <button 
            type="submit" 
            className="auth-button"
          >
            Login
          </button>
        </form>
        
        <p className="auth-link">
          Don't have an account? <Link to="/signup">Sign up here</Link>
        </p>
      </div>
    </div>
  );
};

export default Login;