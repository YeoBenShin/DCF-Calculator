describe('Error Scenarios and Edge Cases', () => {
  beforeEach(() => {
    cy.clearAuth()
  })

  describe('Network and API Errors', () => {
    beforeEach(() => {
      // Mock login for authenticated tests
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()
    })

    it('should handle complete network failure gracefully', () => {
      // Mock network failure
      cy.intercept('GET', '**/financials*', { forceNetworkError: true })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Should show network error message
      cy.contains('Network error. Please check your connection.').should('be.visible')
      cy.get('[data-testid="retry-button"]').should('be.visible')
    })

    it('should handle API timeout errors', () => {
      // Mock timeout
      cy.intercept('GET', '**/financials*', { delay: 15000 }).as('slowRequest')

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Should show loading state initially
      cy.get('[data-testid="loading-spinner"]').should('be.visible')
      
      // Should eventually show timeout error
      cy.contains('Request timed out. Please try again.').should('be.visible')
    })

    it('should handle server errors (500)', () => {
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        error: 'Internal server error'
      }, 500)

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      cy.contains('Server error. Please try again later.').should('be.visible')
    })

    it('should handle malformed API responses', () => {
      // Mock malformed response
      cy.intercept('GET', '**/financials*', {
        statusCode: 200,
        body: 'invalid json response'
      })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      cy.contains('Invalid response from server').should('be.visible')
    })
  })

  describe('Authentication Edge Cases', () => {
    it('should handle expired JWT token', () => {
      // Mock initial login
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'expired-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()

      // Mock API call with expired token error
      cy.mockApiResponse('GET', '**/watchlist', {
        error: 'Token expired'
      }, 401)

      cy.visit('/watchlist')
      
      // Should redirect to login page
      cy.url().should('include', '/login')
      cy.contains('Session expired. Please log in again.').should('be.visible')
    })

    it('should handle invalid token format', () => {
      // Set invalid token in localStorage
      cy.window().then((win) => {
        win.localStorage.setItem('authToken', 'invalid-token-format')
      })

      cy.visit('/watchlist')
      
      // Should redirect to login
      cy.url().should('include', '/login')
    })

    it('should handle missing authentication for protected routes', () => {
      cy.visit('/watchlist')
      
      // Should redirect to login
      cy.url().should('include', '/login')
      cy.contains('Please log in to access this page').should('be.visible')
    })
  })

  describe('Input Validation Edge Cases', () => {
    beforeEach(() => {
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()

      // Mock financial data
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B'],
        free_cash_flow: ['111.5B']
      })
    })

    it('should handle extreme DCF parameter values', () => {
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')

      // Test negative discount rate
      cy.get('[data-testid="discount-rate-input"]').clear().type('-5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.contains('Discount rate must be positive').should('be.visible')

      // Test zero growth rate
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('0')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.contains('Growth rate must be greater than 0').should('be.visible')

      // Test terminal growth rate higher than growth rate
      cy.get('[data-testid="growth-rate-input"]').clear().type('5')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('10')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.contains('Terminal growth rate should not exceed growth rate').should('be.visible')
    })

    it('should handle special characters in ticker input', () => {
      cy.visit('/')
      
      // Test with special characters
      cy.get('[data-testid="ticker-input"]').type('A@PPL#')
      cy.get('[data-testid="search-button"]').click()
      
      cy.contains('Ticker symbol can only contain letters').should('be.visible')
    })

    it('should handle very long ticker symbols', () => {
      cy.visit('/')
      
      // Test with very long ticker
      cy.get('[data-testid="ticker-input"]').type('VERYLONGTICKERSYMBOL')
      cy.get('[data-testid="search-button"]').click()
      
      cy.contains('Ticker symbol too long').should('be.visible')
    })

    it('should handle decimal precision in DCF inputs', () => {
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')

      // Test with many decimal places
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.123456789')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12.987654321')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.555555555')
      
      // Should round to reasonable precision
      cy.get('[data-testid="discount-rate-input"]').should('have.value', '8.12')
      cy.get('[data-testid="growth-rate-input"]').should('have.value', '12.99')
      cy.get('[data-testid="terminal-growth-rate-input"]').should('have.value', '2.56')
    })
  })

  describe('UI State Management Edge Cases', () => {
    beforeEach(() => {
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()
    })

    it('should handle rapid successive API calls', () => {
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B']
      })

      cy.visit('/')
      
      // Make rapid successive searches
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.get('[data-testid="search-button"]').click()
      cy.get('[data-testid="search-button"]').click()
      
      // Should handle gracefully without duplicate requests
      cy.get('[data-testid="loading-spinner"]').should('be.visible')
      cy.get('@GET**/financials*').should('have.been.calledOnce')
    })

    it('should handle browser back/forward navigation', () => {
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B']
      })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      // Navigate to watchlist
      cy.get('[data-testid="nav-watchlist-link"]').click()
      cy.url().should('include', '/watchlist')
      
      // Use browser back button
      cy.go('back')
      cy.url().should('not.include', '/watchlist')
      
      // Should preserve previous search state
      cy.get('[data-testid="ticker-input"]').should('have.value', 'AAPL')
    })

    it('should handle page refresh during API calls', () => {
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B']
      })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Refresh page during loading
      cy.reload()
      
      // Should handle gracefully
      cy.get('[data-testid="ticker-input"]').should('be.visible')
      cy.get('[data-testid="search-button"]').should('be.visible')
    })
  })

  describe('Data Integrity Edge Cases', () => {
    beforeEach(() => {
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })
      cy.login()
    })

    it('should handle missing financial data fields', () => {
      // Mock incomplete financial data
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B'],
        // Missing other required fields
      })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.contains('Incomplete financial data available').should('be.visible')
      cy.get('[data-testid="calculate-dcf-button"]').should('be.disabled')
    })

    it('should handle zero or negative financial values', () => {
      // Mock financial data with problematic values
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['0', '-100B', '200B'],
        free_cash_flow: ['-50B', '0', '100B']
      })

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.contains('Warning: Some financial data contains zero or negative values').should('be.visible')
    })

    it('should handle DCF calculation errors', () => {
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        ticker: 'AAPL',
        revenue: ['365.8B'],
        free_cash_flow: ['111.5B']
      })

      // Mock DCF calculation error
      cy.mockApiResponse('POST', '**/calculateDCF', {
        error: 'Division by zero in DCF calculation'
      }, 400)

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
      
      cy.contains('Error in DCF calculation. Please check your inputs.').should('be.visible')
    })
  })
})