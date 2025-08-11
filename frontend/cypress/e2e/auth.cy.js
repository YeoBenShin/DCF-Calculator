describe('Authentication Flow', () => {
  beforeEach(() => {
    cy.clearAuth()
  })

  describe('User Registration', () => {
    it('should allow new user to sign up successfully', () => {
      // Mock successful registration API response
      cy.mockApiResponse('POST', '**/auth/signup', {
        message: 'User registered successfully',
        token: 'mock-jwt-token'
      })

      const testEmail = `test${Date.now()}@example.com`
      
      cy.visit('/signup')
      
      // Fill out registration form
      cy.get('[data-testid="email-input"]').type(testEmail)
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('password123')
      
      // Submit form
      cy.get('[data-testid="signup-button"]').click()
      
      // Should redirect to main page after successful registration
      cy.url().should('not.include', '/signup')
      cy.url().should('not.include', '/login')
    })

    it('should show error for invalid email format', () => {
      cy.visit('/signup')
      
      cy.get('[data-testid="email-input"]').type('invalid-email')
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('password123')
      
      cy.get('[data-testid="signup-button"]').click()
      
      // Should show validation error
      cy.contains('Please enter a valid email address').should('be.visible')
    })

    it('should show error when passwords do not match', () => {
      cy.visit('/signup')
      
      cy.get('[data-testid="email-input"]').type('test@example.com')
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('differentpassword')
      
      cy.get('[data-testid="signup-button"]').click()
      
      // Should show password mismatch error
      cy.contains('Passwords do not match').should('be.visible')
    })

    it('should handle registration API error', () => {
      // Mock API error response
      cy.mockApiResponse('POST', '**/auth/signup', {
        error: 'Email already exists'
      }, 400)

      cy.visit('/signup')
      
      cy.get('[data-testid="email-input"]').type('existing@example.com')
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('password123')
      
      cy.get('[data-testid="signup-button"]').click()
      
      // Should show API error message
      cy.contains('Email already exists').should('be.visible')
    })
  })

  describe('User Login', () => {
    it('should allow existing user to login successfully', () => {
      // Mock successful login API response
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: {
          email: 'test@example.com',
          id: '123'
        }
      })

      cy.visit('/login')
      
      // Fill out login form
      cy.get('[data-testid="email-input"]').type('test@example.com')
      cy.get('[data-testid="password-input"]').type('password123')
      
      // Submit form
      cy.get('[data-testid="login-button"]').click()
      
      // Should redirect to main page after successful login
      cy.url().should('not.include', '/login')
    })

    it('should show error for invalid credentials', () => {
      // Mock API error response
      cy.mockApiResponse('POST', '**/auth/login', {
        error: 'Invalid credentials'
      }, 401)

      cy.visit('/login')
      
      cy.get('[data-testid="email-input"]').type('wrong@example.com')
      cy.get('[data-testid="password-input"]').type('wrongpassword')
      
      cy.get('[data-testid="login-button"]').click()
      
      // Should show error message
      cy.contains('Invalid credentials').should('be.visible')
    })

    it('should show validation errors for empty fields', () => {
      cy.visit('/login')
      
      // Try to submit without filling fields
      cy.get('[data-testid="login-button"]').click()
      
      // Should show validation errors
      cy.contains('Email is required').should('be.visible')
      cy.contains('Password is required').should('be.visible')
    })
  })

  describe('Logout', () => {
    it('should allow user to logout successfully', () => {
      // Mock login first
      cy.mockApiResponse('POST', '**/auth/login', {
        token: 'mock-jwt-token',
        user: { email: 'test@example.com', id: '123' }
      })

      cy.login()
      
      // Logout
      cy.logout()
      
      // Should redirect to login page
      cy.url().should('include', '/login')
    })
  })
})