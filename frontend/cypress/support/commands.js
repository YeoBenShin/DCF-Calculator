// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

// Custom command to login a user
Cypress.Commands.add('login', (email = 'test@example.com', password = 'password123') => {
  cy.visit('/login')
  cy.get('[data-testid="email-input"]').type(email)
  cy.get('[data-testid="password-input"]').type(password)
  cy.get('[data-testid="login-button"]').click()
  cy.url().should('not.include', '/login')
})

// Custom command to register a new user
Cypress.Commands.add('register', (email = 'newuser@example.com', password = 'password123') => {
  cy.visit('/signup')
  cy.get('[data-testid="email-input"]').type(email)
  cy.get('[data-testid="password-input"]').type(password)
  cy.get('[data-testid="confirm-password-input"]').type(password)
  cy.get('[data-testid="signup-button"]').click()
})

// Custom command to logout
Cypress.Commands.add('logout', () => {
  cy.get('[data-testid="logout-button"]').click()
  cy.url().should('include', '/login')
})

// Custom command to clear local storage and cookies
Cypress.Commands.add('clearAuth', () => {
  cy.clearLocalStorage()
  cy.clearCookies()
})

// Custom command to mock API responses
Cypress.Commands.add('mockApiResponse', (method, url, response, statusCode = 200) => {
  cy.intercept(method, url, {
    statusCode,
    body: response
  })
})

// Custom command to wait for API call
Cypress.Commands.add('waitForApi', (alias) => {
  cy.wait(alias)
})