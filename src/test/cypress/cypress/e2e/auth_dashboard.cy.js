describe('Public Works Portal – Happy path', () => {

  const loginAsAdmin = () => {
    // assuming baseUrl = http://localhost:8080 in cypress.config
    cy.visit('/');

    cy.get('input[slot="input"]', { includeShadowDom: true })
      .eq(0)
      .type('test'); // username

    cy.get('input[slot="input"]', { includeShadowDom: true })
      .eq(1)
      .type('test{enter}'); // password + submit

    cy.url().should('include', '/app');
  };

  it('shows the login page', () => {
    cy.visit('/');
    cy.contains('Public Works Portal');
    cy.get('#login-form').should('exist');
  });

  it('logs in as admin and shows dashboard', () => {
    loginAsAdmin();

    cy.contains('Dashboard', { includeShadowDom: true }).should('exist');
    cy.contains('Open Work Orders', { includeShadowDom: true }).should('exist');
    cy.contains('test', { includeShadowDom: true }).should('exist');
  });

  it('creates a new Work Order', () => {
    // 🔐 log in first
    loginAsAdmin();

    // Ensure dashboard loaded
    cy.contains('Open Work Orders', { includeShadowDom: true }).should('exist');

    // open dialog
    cy.contains('New Work Order', { includeShadowDom: true }).click();
    //add a delay
    cy.wait(500);

    // Title
    cy.get('[data-testid="wo-title"] input[slot="input"]', { includeShadowDom: true })
      .type('newWo');

    // Requested by
    cy.get('[data-testid="wo-requestedBy"] input[slot="input"]', { includeShadowDom: true })
      .type('me');

    // Contact
    cy.get('[data-testid="wo-contact"] input[slot="input"]', { includeShadowDom: true })
      .type('mhgm@gmail.com');

    // Location
    cy.get('[data-testid="wo-location"] input[slot="input"]', { includeShadowDom: true })
      .type('water');

    cy.get('[data-testid="wo-phone"] input[slot="input"]', { includeShadowDom: true })
      .type('7778889999');

    // Description (Vaadin text-area -> <textarea slot="textarea">)
    cy.get('[data-testid="wo-description"] textarea[slot="textarea"]', { includeShadowDom: true })
      .type('description');

    // Save
    cy.get('[data-testid="wo-save"]', { includeShadowDom: true }).click();

    // ✅ Success banner should appear
    cy.get('vaadin-notification-card', { includeShadowDom: true, timeout: 15000 })
      .should('exist')
      .and('contain.text', 'Work order created');

    // Banner actions
    cy.contains('Dismiss', { includeShadowDom: true, timeout: 15000 }).should('be.visible');

    // Dismiss it so it doesn't interfere with later tests
    cy.contains('Dismiss', { includeShadowDom: true }).click({ force: true });
  });
});
