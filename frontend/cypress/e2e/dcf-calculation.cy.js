describe('DCF Calculation Flow', () => {
  beforeEach(() => {
    cy.clearAuth()
    
    // Mock login
    cy.mockApiResponse('POST', '**/auth/login', {
      token: 'mock-jwt-token',
      user: { email: 'test@example.com', id: '123' }
    })
    
    cy.login()
  })

  describe('Stock Search and Financial Data', () => {
    it('should fetch and display financial data for valid ticker', () => {
      // Mock financial data API response
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B', '274.5B', '260.2B', '229.2B'],
        operating_income: ['114.3B', '66.3B', '63.9B', '52.5B'],
        operating_cash_flow: ['122.2B', '80.7B', '77.4B', '69.4B'],
        net_profit: ['99.8B', '57.4B', '55.3B', '45.7B'],
        capital_expenditure: ['10.7B', '7.3B', '7.3B', '13.3B'],
        free_cash_flow: ['111.5B', '73.4B', '70.1B', '56.1B'],
        eps: ['6.16', '3.31', '3.28', '2.97'],
        total_debt: ['123.9B', '112.4B', '106.6B', '91.8B'],
        ordinary_shares_number: ['16.2B', '17.3B', '16.9B', '15.4B'],
        date_fetched: '2025-01-01'
      }

      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)

      cy.visit('/')
      
      // Search for stock
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Wait for financial data to load
      cy.waitForApi('@GET**/financials*')
      
      // Should display financial data
      cy.contains('AAPL').should('be.visible')
      cy.contains('365.8B').should('be.visible') // Revenue
      cy.contains('99.8B').should('be.visible')  // Net profit
    })

    it('should show error for invalid ticker', () => {
      // Mock API error response
      cy.mockApiResponse('GET', '**/financials?ticker=INVALID', {
        error: 'Ticker not found.'
      }, 404)

      cy.visit('/')
      
      cy.get('[data-testid="ticker-input"]').type('INVALID')
      cy.get('[data-testid="search-button"]').click()
      
      // Should show error message
      cy.contains('Ticker not found.').should('be.visible')
    })

    it('should handle network timeout error', () => {
      // Mock network timeout
      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', {
        error: 'Unable to retrieve financials at the moment.'
      }, 500)

      cy.visit('/')
      
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      
      // Should show timeout error message
      cy.contains('Unable to retrieve financials at the moment.').should('be.visible')
    })
  })

  describe('DCF Parameter Input and Calculation', () => {
    beforeEach(() => {
      // Mock financial data for setup
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B', '274.5B', '260.2B', '229.2B'],
        free_cash_flow: ['111.5B', '73.4B', '70.1B', '56.1B'],
        ordinary_shares_number: ['16.2B', '17.3B', '16.9B', '15.4B']
      }

      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)
      
      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
    })

    it('should calculate DCF with valid parameters', () => {
      // Mock DCF calculation API response
      const mockDCFResult = {
        ticker: 'AAPL',
        fair_value_per_share: 173.45,
        current_price: 150.00,
        valuation: 'Undervalued'
      }

      cy.mockApiResponse('POST', '**/calculateDCF', mockDCFResult)

      // Fill DCF parameters
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      
      // Calculate DCF
      cy.get('[data-testid="calculate-dcf-button"]').click()
      
      // Wait for calculation
      cy.waitForApi('@POST**/calculateDCF')
      
      // Should display results
      cy.contains('$173.45').should('be.visible') // Fair value
      cy.contains('$150.00').should('be.visible') // Current price
      cy.contains('Undervalued').should('be.visible')
    })

    it('should show validation error for missing required fields', () => {
      // Clear required fields
      cy.get('[data-testid="discount-rate-input"]').clear()
      cy.get('[data-testid="growth-rate-input"]').clear()
      
      // Try to calculate
      cy.get('[data-testid="calculate-dcf-button"]').click()
      
      // Should show validation errors
      cy.get('[data-testid="discount-rate-input"]').should('have.class', 'error')
      cy.get('[data-testid="growth-rate-input"]').should('have.class', 'error')
      cy.contains('This field is required').should('be.visible')
    })

    it('should show warning for unrealistic growth rate', () => {
      // Input unrealistic growth rate
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('1500') // >1000%
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      
      // Try to calculate
      cy.get('[data-testid="calculate-dcf-button"]').click()
      
      // Should show warning
      cy.contains('Growth rate too high. Please input a realistic value.').should('be.visible')
    })
  })

  describe('Results Display and Charts', () => {
    beforeEach(() => {
      // Setup complete flow
      const mockFinancialData = {
        ticker: 'AAPL',
        revenue: ['365.8B', '274.5B', '260.2B', '229.2B'],
        operating_income: ['114.3B', '66.3B', '63.9B', '52.5B'],
        net_profit: ['99.8B', '57.4B', '55.3B', '45.7B'],
        free_cash_flow: ['111.5B', '73.4B', '70.1B', '56.1B'],
        eps: ['6.16', '3.31', '3.28', '2.97']
      }

      const mockDCFResult = {
        ticker: 'AAPL',
        fair_value_per_share: 173.45,
        current_price: 150.00,
        valuation: 'Undervalued'
      }

      cy.mockApiResponse('GET', '**/financials?ticker=AAPL', mockFinancialData)
      cy.mockApiResponse('POST', '**/calculateDCF', mockDCFResult)

      cy.visit('/')
      cy.get('[data-testid="ticker-input"]').type('AAPL')
      cy.get('[data-testid="search-button"]').click()
      cy.waitForApi('@GET**/financials*')
      
      cy.get('[data-testid="discount-rate-input"]').clear().type('8.5')
      cy.get('[data-testid="growth-rate-input"]').clear().type('12')
      cy.get('[data-testid="terminal-growth-rate-input"]').clear().type('2.5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
    })

    it('should display financial charts correctly', () => {
      // Should display all required charts
      cy.get('[data-testid="revenue-chart"]').should('be.visible')
      cy.get('[data-testid="net-income-chart"]').should('be.visible')
      cy.get('[data-testid="free-cash-flow-chart"]').should('be.visible')
      cy.get('[data-testid="eps-chart"]').should('be.visible')
      
      // Charts should contain data
      cy.get('[data-testid="revenue-chart"]').within(() => {
        cy.contains('365.8B').should('be.visible')
      })
    })

    it('should display fair value card with correct information', () => {
      cy.get('[data-testid="fair-value-card"]').should('be.visible')
      cy.get('[data-testid="fair-value-card"]').within(() => {
        cy.contains('AAPL').should('be.visible')
        cy.contains('$173.45').should('be.visible')
        cy.contains('$150.00').should('be.visible')
        cy.contains('Undervalued').should('be.visible')
      })
    })

    it('should handle overvalued scenario correctly', () => {
      // Mock overvalued result
      const mockOvervaluedResult = {
        ticker: 'AAPL',
        fair_value_per_share: 120.00,
        current_price: 150.00,
        valuation: 'Overvalued'
      }

      cy.mockApiResponse('POST', '**/calculateDCF', mockOvervaluedResult)
      
      // Recalculate with different parameters
      cy.get('[data-testid="growth-rate-input"]').clear().type('5')
      cy.get('[data-testid="calculate-dcf-button"]').click()
      cy.waitForApi('@POST**/calculateDCF')
      
      // Should show overvalued
      cy.contains('Overvalued').should('be.visible')
      cy.get('[data-testid="fair-value-card"]').should('have.class', 'overvalued')
    })
  })
})