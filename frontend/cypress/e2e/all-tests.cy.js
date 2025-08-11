describe('DCF Calculator - Complete End-to-End Test Suite', () => {
  before(() => {
    // Setup test environment
    cy.log('Starting complete E2E test suite for DCF Calculator')
  })

  beforeEach(() => {
    // Clear state before each test
    cy.clearAuth()
  })

  describe('Complete User Journey', () => {
    it('should complete full user journey from registration to watchlist management', () => {
      // Step 1: User Registration
      cy.mockApiResponse('POST', '**/auth/signup', {
        message: 'User registered successfully',
        token: 'mock-jwt-token'
      })

      const testEmail = `e2etest${Date.now()}@example.com`
      
      cy.visit('/signup')
      cy.get('[data-testid="email-input"]').type(testEmail)
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('password123')
      cy.get('[data-testid="signup-button"]').click()
      
      // Should be logged in after registration
      cy.url().should('not.include', '/signup')
      cy.url().should('not.include', '/login')

      // Step 2: Stock Search and Financial Data Retrieval
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B', '274.5B', '260.2B', '229.2B'],
        operating_income: ['114.3B', '66.3B', '63.9B', '52.5B'],
        net_profit: ['99.8B', '57.4B', '55.3B', '45.7B'],
        free_cash_flow: ['111.5B', '73.4B', '70.1B', '56.1B'],
        eps: ['6.16', '3.31', '3.28', '2.97'],
        ordinary_shares_number: ['16.2B', '17.3B', '16.9B', '15.4B']
      }

      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)

      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')

      // Verify financial data is displayed
      cy.contains('AAPL').should('be.visible')
      cy.contains('365.8B').should('be.visible')

      // Step 3: DCF Calculation
      const mockDCFResult = {
        ticker: 'AAPL',
        fair_value_per_share: 173.45,
        current_price: 150.00,
        valuation: 'Undervalued'
      }

      cy.mockApiResponse('POST', '**/calculateDCF', mockDCFResult)

      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')

      // Verify DCF results
      cy.contains('$173.45').should('be.visible')
      cy.contains('$150.00').should('be.visible')
      cy.contains('Undervalued').should('be.visible')

      // Step 4: Add to Watchlist
      cy.mockApiResponse('POST', '**/watchlist/add', {
        message: 'Stock added to watchlist successfully'
      })

      cy.get('[data-testid="add-to-watchlist-button"]').click()
      cy.waitForApi('@POST**/watchlist/add')
      cy.contains('Stock added to watchlist successfully').should('be.visible')

      // Step 5: Navigate to Watchlist and Verify
      const mockWatchlist = {
        watchlist: [
          {
            ticker: 'AAPL',
            fair_value_per_share: 173.45,
            current_price: 150.00,
            valuation: 'Undervalued',
            last_updated: '2025-01-01T10:00:00Z'
          }
        ]
      }

      cy.mockApiResponse('GET', '**/watchlist', mockWatchlist)

      cy.get('[data-testid="nav-watchlist-link"]').click()
      cy.url().should('include', '/watchlist')
      cy.waitForApi('@GET**/watchlist')

      // Verify watchlist contains the stock
      cy.get('[data-testid="watchlist-item-AAPL"]').should('be.visible')
      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.contains('AAPL').should('be.visible')
        cy.contains('$173.45').should('be.visible')
        cy.contains('Undervalued').should('be.visible')
      })

      // Step 6: Remove from Watchlist
      cy.mockApiResponse('DELETE', '**/watchlist/remove', {
        message: 'Stock removed from watchlist successfully'
      })

      cy.mockApiResponse('GET', '**/watchlist', { watchlist: [] })

      cy.get('[data-testid="watchlist-item-AAPL"]').within(() => {
        cy.get('[data-testid="remove-button"]').click()
      })
      cy.get('[data-testid="confirm-remove-button"]').click()
      cy.waitForApi('@DELETE**/watchlist/remove')

      // Verify removal
      cy.contains('Stock removed from watchlist successfully').should('be.visible')
      cy.get('[data-testid="watchlist-item-AAPL"]').should('not.exist')

      // Step 7: Logout
      cy.get('[data-testid="logout-button"]').click()
      cy.url().should('include', '/login')

      cy.log('Complete user journey test passed successfully')
    })
  })

  describe('Error Recovery Scenarios', () => {
    it('should handle and recover from various error scenarios', () => {
      // Login first
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()

      // Test 1: Invalid ticker error and recovery
      cy.mockApiResponse('GET', '**/financials?ticker=INVALID', {
        error: 'Ticker not found.'
      }, 404)

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('INVALID')
      cy.get('[data-testid="search-button"]').click()
      cy.contains('Ticker not found.').should('be.visible')

      // Recovery: Search for valid ticker
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B'],
        free_cash_flow: ['111.5B']
      }
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)

      cy.get('[data-testid="ticker-input"]').clear().type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      cy.contains('AAPL').should('be.visible')

      // Test 2: DCF calculation error and recovery
      cy.mockApiResponse('POST', '**/calculateDCF', {
        error: 'Invalid growth rate'
      }, 400)

      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('2000') // Invalid high rate
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.contains('Growth rate too high').should('be.visible')

      // Recovery: Use valid parameters
      const mockDCFResult = {
        ticker: 'AAPL',
        fair_value_per_share: 173.45,
        current_price: 150.00,
        valuation: 'Undervalued'
      }
      cy.mockApiResponse('POST', '**/calculateDCF', mockDCFResult)

      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
      cy.contains('$173.45').should('be.visible')

      cy.log('Error recovery scenarios test passed successfully')
    })
  })

  describe('Performance and Load Scenarios', () => {
    it('should handle multiple rapid interactions gracefully', () => {
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()

      // Mock responses with delays to simulate real-world conditions
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B']
      })

      cy.visit('/')

      // Rapid successive searches
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.get('[data-testid="search-button"]').click() // Second click should be ignored
      cy.get('[data-testid="search-button"]').click() // Third click should be ignored

      // Should only make one API call
      cy.get('@GET**/financials*').should('have.been.calledOnce')

      cy.log('Performance and load scenarios test passed successfully')
    })
  })

  after(() => {
    cy.log('Complete E2E test suite finished')
  })
})