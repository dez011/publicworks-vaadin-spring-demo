describe('Happy path', function() {
	beforeEach(() => {
		// preserve, of re-visit
		// Cypress.Cookies.preserveOnce('JSESSIONID')
		cy.visit("/")
	})
	it('Homepage has content', () => {
		cy.contains("Greeting Service")
	})
	it('Greet the fallback', () => {
		cy.get('#greet-button')
			.click()
		cy.contains('Hello, World')
	})
	it('Greet something', () => {
		cy.get('#name-input')
			.find("input")
			.type("Something")
		cy.get('#greet-button')
			.click()
		cy.contains('Hello, Something')
	})
});

it('newtest', function() {
	cy.visit('localhost:8080')
	
});
