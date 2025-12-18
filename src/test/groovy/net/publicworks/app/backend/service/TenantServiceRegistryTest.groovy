package net.publicworks.app.backend.service

import net.publicworks.app.backend.itf.Customer
import net.publicworks.app.backend.itf.CustomerDiferentiator
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class TenantServiceRegistryTest extends Specification {

    interface TestService {}

    @Customer(CustomerDiferentiator.DEFAULT)
    static class DefaultService implements TestService {}

    @Customer(CustomerDiferentiator.ALASKA)
    static class AlaskaService implements TestService {}

    ApplicationContext applicationContext = Mock()
    _TenantServiceRegistry registry = new _TenantServiceRegistry(applicationContext)

    def "returns tenant-specific service when available"() {
        given: "two beans implementing the same interface, one default and one for Alaska"
        def defaultSvc = new DefaultService()
        def alaskaSvc = new AlaskaService()

        applicationContext.getBeansOfType(TestService) >> [
                "defaultService": defaultSvc,
                "alaskaService" : alaskaSvc
        ]

        when: "we ask for the Alaska tenant"
        def result = registry.get(TestService, "alaska")

        then: "the Alaska-specific service is returned"
        result.is(alaskaSvc)
    }

    def "falls back to default service when tenant-specific implementation is missing"() {
        given: "only default implementation is registered"
        def defaultSvc = new DefaultService()

        applicationContext.getBeansOfType(TestService) >> [
                "defaultService": defaultSvc
        ]

        when: "we ask for some tenant with no specific implementation"
        def result = registry.get(TestService, "texas")

        then: "the default implementation is used"
        result.is(defaultSvc)
    }

    def "throws when no beans exist for a given type"() {
        given:
        applicationContext.getBeansOfType(TestService) >> [:] // empty map

        when:
        registry.get(TestService, "alaska")

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("No bean found for type")
    }
}
