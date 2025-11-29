/// <reference types="cypress" />

describe('Public Works Portal – Happy path', () => {
  beforeEach(() => {
    // assuming baseUrl = http://localhost:8080 in cypress.config.{js,ts}
    cy.visit('/');
  });

  it('shows the login page', () => {
    cy.contains('Public Works Portal');
    cy.get('#login-form').should('exist');
  });

  it('logs in as admin and shows dashboard', () => {
    // Fill in username & password (Vaadin input, slot-based, shadow-DOM aware)
    cy.get('input[slot="input"]', { includeShadowDom: true })
      .eq(0)
      .type('test'); // username

    cy.get('input[slot="input"]', { includeShadowDom: true })
      .eq(1)
      .type('test{enter}'); // password + submit

    // Verify we landed on dashboard
    cy.url().should('include', '/app');
    cy.contains('Dashboard').should('exist');
    cy.contains('Open Work Orders').should('exist');

    // Verify the logged-in user is shown
    cy.contains('test').should('exist');
  });
});
