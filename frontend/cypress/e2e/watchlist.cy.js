describe('Watchlist Functionality', () => {
  beforeEach(() => {
    cy.clearAuth()
    
    // Mock login
    cy.mockApiResponse('POST', '**/auth/login', {
      token: 'mock-jwt-token',
      user: { email: 'test@example.com', id: '123' }
    })
    
    cy.login()
  })

  describe('Watchlist Page', () => {
    it('should display empty watchlist for new user', () => {
      // Mock empty watchlist API response
      cy.mockApiResponse('GET', '**/watchlist', {
        watchlist: []
      })

      cy.visit('/watchlist')
      
      // Should show empty state
      cy.contains('Your watchlist is empty').should('be.visible')
      cy.contains('Add some stocks to get started').should('be.visible')
    })

    it('should display existing watchlist items with fair value status', () => {
      // Mock watchlist with items
      const mockWatchlist = {
        watchlist: [
          {
            ticker: 'AAPL',
            fair_value_per_share: 173.45,
            current_price: 150.00,
            valuation: 'Undervalued',
            last_updated: '2025-01-01T10:00:00Z'
          },
          {
            ticker: 'GOOGL',
            fair_value_per_share: 2800.00,
            current_price: 2950.00,
            valuation: 'Overvalued',
            last_updated: '2025-01-01T10:00:00Z'
          }
        ]
      }

      cy.mockApiResponse('GET', '**/watchlist', mockWatchlist)

      cy.visit('/watchlist')
      
      // Should display watchlist items
      cy.get('[data-testid="watchlist-item"]').should('have.length', 2)
      
      // Check AAPL item
      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.contains('AAPL').should('be.visible')
        cy.contains('$173.45').should('be.visible')
        cy.contains('$150.00').should('be.visible')
        cy.contains('Undervalued').should('be.visible')
      })
      
      // Check GOOGL item
      cy.get('[data-testid="watchlist-item-GOOGL"]').within(() => {
        cy.contains('GOOGL').should('be.visible')
        cy.contains('$2800.00').should('be.visible')
        cy.contains('$2950.00').should('be.visible')
        cy.contains('Overvalued').should('be.visible')
      })
    })

    it('should handle watchlist API error gracefully', () => {
      // Mock API error
      cy.mockApiResponse('GET', '**/watchlist', {
        error: 'Failed to load watchlist'
      }, 500)

      cy.visit('/watchlist')
      
      // Should show error message
      cy.contains('Failed to load watchlist').should('be.visible')
      cy.get('[data-testid="retry-button"]').should('be.visible')
    })
  })

  describe('Add Stock to Watchlist', () => {
    beforeEach(() => {
      // Mock financial data and DCF calculation for setup
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B', '274.5B', '260.2B', '229.2B']
      }

      const mockDCFResult = {
        ticker: 'AAPL',
        fair_value_per_share: 173.45,
        current_price: 150.00,
        valuation: 'Undervalued'
      }

      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)
      cy.mockApiResponse('POST', '**/calculateDCF', mockDCFResult)
    })

    it('should add stock to watchlist from results page', () => {
      // Mock successful add to watchlist
      cy.mockApiResponse('POST', '**/watchlist/add', {
        message: 'Stock added to watchlist successfully'
      })

      // Navigate to results page
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
      
      // Add to watchlist
      cy.get('[data-testid="add-to-watchlist-button"]').click()
      cy.waitForApi('@POST**/watchlist/add')
      
      // Should show success message
      cy.contains('Stock added to watchlist successfully').should('be.visible')
      
      // Button should change to indicate added
      cy.get('[data-testid="add-to-watchlist-button"]').should('contain', 'Added to Watchlist')
      cy.get('[data-testid="add-to-watchlist-button"]').should('be.disabled')
    })

    it('should handle duplicate stock addition gracefully', () => {
      // Mock duplicate error
      cy.mockApiResponse('POST', '**/watchlist/add', {
        error: 'Stock already in watchlist'
      }, 400)

      // Navigate to results page
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
      
      // Try to add to watchlist
      cy.get('[data-testid="add-to-watchlist-button"]').click()
      cy.waitForApi('@POST**/watchlist/add')
      
      // Should show error message
      cy.contains('Stock already in watchlist').should('be.visible')
    })

    it('should require authentication to add to watchlist', () => {
      // Clear auth and try to add
      cy.clearAuth()
      
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Add to watchlist button should not be visible or should redirect to login
      cy.get('[data-testid="add-to-watchlist-button"]').should('not.exist')
      // OR
      // cy.get('[data-testid="add-to-watchlist-button"]').click()
      // cy.url().should('include', '/login')
    })
  })

  describe('Remove Stock from Watchlist', () => {
    beforeEach(() => {
      // Mock watchlist with items
      const mockWatchlist = {
        watchlist: [
          {
            ticker: 'AAPL',
            fair_value_per_share: 173.45,
            current_price: 150.00,
            valuation: 'Undervalued',
            last_updated: '2025-01-01T10:00:00Z'
          },
          {
            ticker: 'GOOGL',
            fair_value_per_share: 2800.00,
            current_price: 2950.00,
            valuation: 'Overvalued',
            last_updated: '2025-01-01T10:00:00Z'
          }
        ]
      }

      cy.mockApiResponse('GET', '**/watchlist', mockWatchlist)
    })

    it('should remove stock from watchlist successfully', () => {
      // Mock successful removal
      cy.mockApiResponse('DELETE', '**/watchlist/remove', {
        message: 'Stock removed from watchlist successfully'
      })

      // Mock updated watchlist after removal
      const updatedWatchlist = {
        watchlist: [
          {
            ticker: 'GOOGL',
            fair_value_per_share: 2800.00,
            current_price: 2950.00,
            valuation: 'Overvalued',
            last_updated: '2025-01-01T10:00:00Z'
          }
        ]
      }
      cy.mockApiResponse('GET', '**/watchlist', updatedWatchlist)

      cy.visit('/watchlist')
      
      // Remove AAPL from watchlist
      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.get('[data-testid="remove-button"]').click()
      })
      
      // Confirm removal in modal/dialog
      cy.get('[data-testid="confirm-remove-button"]').click()
      cy.waitForApi('@DELETE**/watchlist/remove')
      
      // Should show success message
      cy.contains('Stock removed from watchlist successfully').should('be.visible')
      
      // AAPL should no longer be visible
      cy.get('[data-testid="watchlist-item-AAPL"]').should('not.exist')
      cy.get('[data-testid="watchlist-item-GOOGL"]').should('exist')
    })

    it('should handle removal API error', () => {
      // Mock API error
      cy.mockApiResponse('DELETE', '**/watchlist/remove', {
        error: 'Failed to remove stock from watchlist'
      }, 500)

      cy.visit('/watchlist')
      
      // Try to remove AAPL
      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.get('[data-testid="remove-button"]').click()
      })
      
      cy.get('[data-testid="confirm-remove-button"]').click()
      cy.waitForApi('@DELETE**/watchlist/remove')
      
      // Should show error message
      cy.contains('Failed to remove stock from watchlist').should('be.visible')
      
      // Stock should still be visible
      cy.get('[data-testid="watchlist-item-AAPL"]').should('exist')
    })

    it('should allow canceling removal', () => {
      cy.visit('/watchlist')
      
      // Start removal process
      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.get('[data-testid="remove-button"]').click()
      })
      
      // Cancel removal
      cy.get('[data-testid="cancel-remove-button"]').click()
      
      // Stock should still be visible
      cy.get('[data-testid="watchlist-item-AAPL"]').should('exist')
      cy.get('[data-testid="watchlist-item-GOOGL"]').should('exist')
    })
  })

  describe('Watchlist Navigation', () => {
    it('should navigate to watchlist from main navigation', () => {
      cy.visit('/')
      
      // Click watchlist link in navigation
      cy.get('[data-testid="nav-watchlist-link"]').click()
      
      // Should navigate to watchlist page
      cy.url().should('include', '/watchlist')
    })

    it('should navigate back to calculator from watchlist', () => {
      cy.visit('/watchlist')
      
      // Click calculator link in navigation
      cy.get('[data-testid="nav-calculator-link"]').click()
      
      // Should navigate to main page
      cy.url().should('not.include', '/watchlist')
    })
  })
})