import React, { useState, useEffect } from 'react';
import { watchlistService } from '../../services/watchlistService';
import { authService } from '../../services/authService';
import { WatchlistItem } from '../../types';
import './Watchlist.css';

const Watchlist: React.FC = () => {
  const [watchlistItems, setWatchlistItems] = useState<WatchlistItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [newTicker, setNewTicker] = useState<string>('');
  const [addingStock, setAddingStock] = useState<boolean>(false);
  const [removingStock, setRemovingStock] = useState<string>('');

  useEffect(() => {
    loadWatchlist();
  }, []);

  const loadWatchlist = async () => {
    try {
      setLoading(true);
      setError('');
      
      if (!authService.isAuthenticated()) {
        setError('Please log in to view your watchlist');
        return;
      }

      const items = await watchlistService.getWatchlist();
      setWatchlistItems(items);
    } catch (err: any) {
      setError(err.message || 'Failed to load watchlist');
    } finally {
      setLoading(false);
    }
  };

  const handleAddStock = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!newTicker.trim()) {
      setError('Please enter a stock ticker');
      return;
    }

    try {
      setAddingStock(true);
      setError('');
      setSuccess('');

      await watchlistService.addToWatchlist(newTicker.trim());
      setSuccess(`${newTicker.toUpperCase()} added to watchlist successfully!`);
      setNewTicker('');
      
      // Reload watchlist to show the new item
      await loadWatchlist();
    } catch (err: any) {
      setError(err.message || 'Failed to add stock to watchlist');
    } finally {
      setAddingStock(false);
    }
  };

  const handleRemoveStock = async (ticker: string) => {
    try {
      setRemovingStock(ticker);
      setError('');
      setSuccess('');

      await watchlistService.removeFromWatchlist(ticker);
      setSuccess(`${ticker} removed from watchlist successfully!`);
      
      // Remove the item from local state immediately for better UX
      setWatchlistItems(prev => prev.filter(item => item.ticker !== ticker));
    } catch (err: any) {
      setError(err.message || 'Failed to remove stock from watchlist');
    } finally {
      setRemovingStock('');
    }
  };

  const formatPrice = (price: number | undefined): string => {
    if (price === undefined || price === null) return 'N/A';
    return `$${price.toFixed(2)}`;
  };

  const formatDate = (dateString: string | undefined): string => {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return 'N/A';
      }
      return date.toLocaleDateString();
    } catch {
      return 'N/A';
    }
  };

  const getValuationBadgeClass = (valuation: string | undefined): string => {
    if (!valuation) return '';
    
    switch (valuation.toLowerCase()) {
      case 'undervalued':
        return 'valuation-undervalued';
      case 'overvalued':
        return 'valuation-overvalued';
      case 'fair value':
        return 'valuation-fair';
      default:
        return '';
    }
  };

  const clearMessages = () => {
    setError('');
    setSuccess('');
  };

  if (!authService.isAuthenticated()) {
    return (
      <div className="watchlist-container">
        <div className="error-message">
          Please log in to view your watchlist.
        </div>
      </div>
    );
  }

  return (
    <div className="watchlist-container">
      <div className="watchlist-header">
        <h1 className="watchlist-title">My Watchlist</h1>
        
        <form onSubmit={handleAddStock} className="add-stock-section">
          <input
            type="text"
            value={newTicker}
            onChange={(e) => {
              setNewTicker(e.target.value);
              clearMessages();
            }}
            placeholder="Enter stock ticker (e.g., AAPL)"
            className="add-stock-input"
            disabled={addingStock}
            maxLength={10}
          />
          <button
            type="submit"
            className="add-stock-button"
            disabled={addingStock || !newTicker.trim()}
          >
            {addingStock ? 'Adding...' : 'Add Stock'}
          </button>
        </form>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {success && (
        <div className="success-message">
          {success}
        </div>
      )}

      {loading ? (
        <div className="loading-container">
          <div className="loading-spinner" data-testid="loading-spinner"></div>
        </div>
      ) : watchlistItems.length === 0 ? (
        <div className="empty-watchlist">
          <div className="empty-watchlist-icon">📊</div>
          <h2 className="empty-watchlist-title">Your watchlist is empty</h2>
          <p className="empty-watchlist-subtitle">
            Add stocks to track their fair value and performance over time
          </p>
        </div>
      ) : (
        <table className="watchlist-table">
          <thead>
            <tr>
              <th>Ticker</th>
              <th>Fair Value</th>
              <th>Current Price</th>
              <th>Valuation</th>
              <th>Last Updated</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {watchlistItems.map((item) => (
              <tr key={item.ticker}>
                <td className="ticker-cell">{item.ticker}</td>
                <td className="price-cell">{formatPrice(item.fair_value_per_share)}</td>
                <td className="price-cell">{formatPrice(item.current_price)}</td>
                <td>
                  {item.valuation ? (
                    <span className={`valuation-badge ${getValuationBadgeClass(item.valuation)}`}>
                      {item.valuation}
                    </span>
                  ) : (
                    'N/A'
                  )}
                </td>
                <td className="last-updated">{formatDate(item.last_updated)}</td>
                <td>
                  <button
                    onClick={() => handleRemoveStock(item.ticker)}
                    className="remove-button"
                    disabled={removingStock === item.ticker}
                  >
                    {removingStock === item.ticker ? 'Removing...' : 'Remove'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default Watchlist;