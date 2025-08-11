import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Watchlist from '../Watchlist';
import { watchlistService } from '../../../services/watchlistService';
import { authService } from '../../../services/authService';
import { WatchlistItem } from '../../../types';

// Mock the services
jest.mock('../../../services/watchlistService');
jest.mock('../../../services/authService');

const mockWatchlistService = watchlistService as jest.Mocked<typeof watchlistService>;
const mockAuthService = authService as jest.Mocked<typeof authService>;

const mockWatchlistItems: WatchlistItem[] = [
  {
    ticker: 'AAPL',
    fair_value_per_share: 175.50,
    current_price: 150.00,
    valuation: 'Undervalued',
    last_updated: '2025-01-08T10:00:00Z'
  },
  {
    ticker: 'GOOGL',
    fair_value_per_share: 2800.00,
    current_price: 2900.00,
    valuation: 'Overvalued',
    last_updated: '2025-01-08T09:30:00Z'
  },
  {
    ticker: 'MSFT',
    fair_value_per_share: 420.00,
    current_price: 420.00,
    valuation: 'Fair Value',
    last_updated: '2025-01-08T11:15:00Z'
  }
];

describe('Watchlist Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAuthService.isAuthenticated.mockReturnValue(true);
  });

  describe('Authentication', () => {
    it('should show login message when user is not authenticated', () => {
      mockAuthService.isAuthenticated.mockReturnValue(false);
      
      render(<Watchlist />);
      
      expect(screen.getByText('Please log in to view your watchlist.')).toBeInTheDocument();
    });

    it('should load watchlist when user is authenticated', async () => {
      mockWatchlistService.getWatchlist.mockResolvedValue(mockWatchlistItems);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(mockWatchlistService.getWatchlist).toHaveBeenCalled();
      });
    });
  });

  describe('Loading States', () => {
    it('should show loading spinner while fetching watchlist', () => {
      mockWatchlistService.getWatchlist.mockImplementation(() => new Promise(() => {}));
      
      render(<Watchlist />);
      
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('should hide loading spinner after data is loaded', async () => {
      mockWatchlistService.getWatchlist.mockResolvedValue(mockWatchlistItems);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
      });
    });
  });

  describe('Watchlist Display', () => {
    beforeEach(() => {
      mockWatchlistService.getWatchlist.mockResolvedValue(mockWatchlistItems);
    });

    it('should display watchlist items in a table', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('AAPL')).toBeInTheDocument();
        expect(screen.getByText('GOOGL')).toBeInTheDocument();
        expect(screen.getByText('MSFT')).toBeInTheDocument();
      });
    });

    it('should display fair values correctly formatted', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('$175.50')).toBeInTheDocument();
        expect(screen.getByText('$2800.00')).toBeInTheDocument();
        expect(screen.getAllByText('$420.00')).toHaveLength(2); // Fair value and current price for MSFT
      });
    });

    it('should display current prices correctly formatted', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('$150.00')).toBeInTheDocument();
        expect(screen.getByText('$2900.00')).toBeInTheDocument();
      });
    });

    it('should display valuation badges with correct styling', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        const undervaluedBadge = screen.getByText('Undervalued');
        const overvaluedBadge = screen.getByText('Overvalued');
        const fairValueBadges = screen.getAllByText('Fair Value');
        const fairValueBadge = fairValueBadges.find(badge => badge.classList.contains('valuation-badge'));
        
        expect(undervaluedBadge).toHaveClass('valuation-undervalued');
        expect(overvaluedBadge).toHaveClass('valuation-overvalued');
        expect(fairValueBadge).toHaveClass('valuation-fair');
      });
    });

    it('should display formatted dates', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        // Dates should be formatted as locale date strings
        expect(screen.getAllByText('1/8/2025')).toHaveLength(3);
      });
    });

    it('should show empty state when watchlist is empty', async () => {
      mockWatchlistService.getWatchlist.mockResolvedValue([]);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('Your watchlist is empty')).toBeInTheDocument();
        expect(screen.getByText('Add stocks to track their fair value and performance over time')).toBeInTheDocument();
      });
    });
  });

  describe('Add Stock Functionality', () => {
    beforeEach(() => {
      mockWatchlistService.getWatchlist.mockResolvedValue([]);
    });

    it('should have add stock form with input and button', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)')).toBeInTheDocument();
        expect(screen.getByText('Add Stock')).toBeInTheDocument();
      });
    });

    it('should update input value when typing', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)') as HTMLInputElement;
        fireEvent.change(input, { target: { value: 'AAPL' } });
        expect(input.value).toBe('AAPL');
      });
    });

    it('should disable button when input is empty', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        const button = screen.getByText('Add Stock') as HTMLButtonElement;
        expect(button).toBeDisabled();
      });
    });

    it('should enable button when input has value', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
        const button = screen.getByText('Add Stock') as HTMLButtonElement;
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        expect(button).not.toBeDisabled();
      });
    });

    it('should call addToWatchlist service when form is submitted', async () => {
      mockWatchlistService.addToWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
        const button = screen.getByText('Add Stock');
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        fireEvent.click(button);
        
        expect(mockWatchlistService.addToWatchlist).toHaveBeenCalledWith('AAPL');
      });
    });

    it('should show success message after adding stock', async () => {
      mockWatchlistService.addToWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
        const button = screen.getByText('Add Stock');
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        fireEvent.click(button);
      });
      
      await waitFor(() => {
        expect(screen.getByText('AAPL added to watchlist successfully!')).toBeInTheDocument();
      });
    });

    it('should clear input after successful addition', async () => {
      mockWatchlistService.addToWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)') as HTMLInputElement;
        const button = screen.getByText('Add Stock');
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        fireEvent.click(button);
      });
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)') as HTMLInputElement;
        expect(input.value).toBe('');
      });
    });

    it('should show error message when adding stock fails', async () => {
      mockWatchlistService.addToWatchlist.mockRejectedValue(new Error('Stock already in watchlist'));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
        const button = screen.getByText('Add Stock');
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        fireEvent.click(button);
      });
      
      await waitFor(() => {
        expect(screen.getByText('Stock already in watchlist')).toBeInTheDocument();
      });
    });

    it('should show loading state while adding stock', async () => {
      mockWatchlistService.addToWatchlist.mockImplementation(() => new Promise(() => {}));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
        const button = screen.getByText('Add Stock');
        
        fireEvent.change(input, { target: { value: 'AAPL' } });
        fireEvent.click(button);
        
        expect(screen.getByText('Adding...')).toBeInTheDocument();
      });
    });
  });

  describe('Remove Stock Functionality', () => {
    beforeEach(() => {
      mockWatchlistService.getWatchlist.mockResolvedValue(mockWatchlistItems);
    });

    it('should have remove buttons for each stock', async () => {
      render(<Watchlist />);
      
      await waitFor(() => {
        const removeButtons = screen.getAllByText('Remove');
        expect(removeButtons).toHaveLength(3);
      });
    });

    it('should call removeFromWatchlist service when remove button is clicked', async () => {
      mockWatchlistService.removeFromWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const removeButtons = screen.getAllByText('Remove');
        fireEvent.click(removeButtons[0]);
        
        expect(mockWatchlistService.removeFromWatchlist).toHaveBeenCalledWith('AAPL');
      });
    });

    it('should show success message after removing stock', async () => {
      mockWatchlistService.removeFromWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const removeButtons = screen.getAllByText('Remove');
        fireEvent.click(removeButtons[0]);
      });
      
      await waitFor(() => {
        expect(screen.getByText('AAPL removed from watchlist successfully!')).toBeInTheDocument();
      });
    });

    it('should remove stock from UI immediately after successful removal', async () => {
      mockWatchlistService.removeFromWatchlist.mockResolvedValue();
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('AAPL')).toBeInTheDocument();
      });
      
      const removeButtons = screen.getAllByText('Remove');
      fireEvent.click(removeButtons[0]);
      
      await waitFor(() => {
        expect(screen.queryByText('AAPL')).not.toBeInTheDocument();
      });
    });

    it('should show error message when removing stock fails', async () => {
      mockWatchlistService.removeFromWatchlist.mockRejectedValue(new Error('Stock not found in watchlist'));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const removeButtons = screen.getAllByText('Remove');
        fireEvent.click(removeButtons[0]);
      });
      
      await waitFor(() => {
        expect(screen.getByText('Stock not found in watchlist')).toBeInTheDocument();
      });
    });

    it('should show loading state while removing stock', async () => {
      mockWatchlistService.removeFromWatchlist.mockImplementation(() => new Promise(() => {}));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const removeButtons = screen.getAllByText('Remove');
        fireEvent.click(removeButtons[0]);
        
        expect(screen.getByText('Removing...')).toBeInTheDocument();
      });
    });
  });

  describe('Error Handling', () => {
    it('should show error message when watchlist loading fails', async () => {
      mockWatchlistService.getWatchlist.mockRejectedValue(new Error('Server error'));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('Server error')).toBeInTheDocument();
      });
    });

    it('should show validation error when trying to add empty ticker', async () => {
      mockWatchlistService.getWatchlist.mockResolvedValue([]);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const button = screen.getByText('Add Stock') as HTMLButtonElement;
        expect(button).toBeDisabled();
        
        // Try to submit the form directly to test validation
        const form = button.closest('form');
        if (form) {
          fireEvent.submit(form);
          expect(screen.getByText('Please enter a stock ticker')).toBeInTheDocument();
        }
      });
    });

    it('should clear error messages when user starts typing', async () => {
      mockWatchlistService.getWatchlist.mockRejectedValue(new Error('Server error'));
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('Server error')).toBeInTheDocument();
      });
      
      const input = screen.getByPlaceholderText('Enter stock ticker (e.g., AAPL)');
      fireEvent.change(input, { target: { value: 'A' } });
      
      expect(screen.queryByText('Server error')).not.toBeInTheDocument();
    });
  });

  describe('Data Formatting', () => {
    it('should display N/A for undefined prices', async () => {
      const itemsWithMissingData: WatchlistItem[] = [
        {
          ticker: 'TEST',
          fair_value_per_share: undefined,
          current_price: undefined,
          valuation: undefined,
          last_updated: undefined
        }
      ];
      
      mockWatchlistService.getWatchlist.mockResolvedValue(itemsWithMissingData);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        const naElements = screen.getAllByText('N/A');
        expect(naElements.length).toBeGreaterThan(0);
      });
    });

    it('should handle invalid date strings gracefully', async () => {
      const itemsWithInvalidDate: WatchlistItem[] = [
        {
          ticker: 'TEST',
          fair_value_per_share: 100,
          current_price: 95,
          valuation: 'Undervalued',
          last_updated: 'invalid-date'
        }
      ];
      
      mockWatchlistService.getWatchlist.mockResolvedValue(itemsWithInvalidDate);
      
      render(<Watchlist />);
      
      await waitFor(() => {
        expect(screen.getByText('N/A')).toBeInTheDocument();
      });
    });
  });
});