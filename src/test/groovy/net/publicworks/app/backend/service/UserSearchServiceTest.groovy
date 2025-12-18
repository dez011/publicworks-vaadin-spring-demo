package net.publicworks.app.backend.service

import net.publicworks.app.VaadinApplication
import net.publicworks.app.backend.entity.User
import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.repo.IUserRepository
import net.publicworks.app.backend.service.search.SearchFilter
import net.publicworks.app.backend.service.search.SearchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Unroll

import static net.publicworks.app.backend.service.search.SearchFilter.Operator.*

@SpringBootTest(classes = VaadinApplication)
@ActiveProfiles("dev")
class UserSearchServiceTest extends Specification {

    @Autowired
    SearchService searchService

    @Autowired
    IUserRepository userRepo

    def setup() {
        // start from a clean slate
        userRepo.deleteAll()

        // seed some users
        userRepo.saveAll([
                new User().tap {
                    email = "alice@example.com"
                    passwordHash = "hash-a"
                    role = "ADMIN"
                    tenantId = "default"
                    customerDiferentiator = CustomerDiferentiator.DEFAULT
                },
                new User().tap {
                    email = "bob@example.com"
                    passwordHash = "hash-b"
                    role = "USER"
                    tenantId = "default"
                    customerDiferentiator = CustomerDiferentiator.DEFAULT
                },
                new User().tap {
                    email = "charlie@test.gov"
                    passwordHash = "hash-c"
                    role = "USER"
                    tenantId = "default"
                    customerDiferentiator = CustomerDiferentiator.DEFAULT
                }
        ])
    }

    @Unroll
    def "user dynamic search scenario: #description"() {
        given:
        def pageable = PageRequest.of(0, 10)

        when:
        Page<User> page = searchService.search(User.class, filters as List, pageable)

        then:
        page.totalElements == expectedCount
        (page.content*.email as Set) == (expectedEmails as Set)

        where:
        description                              | filters                                             | expectedCount | expectedEmails
        "no filters returns all"                 | []                                                  | 3             | ["alice@example.com", "bob@example.com", "charlie@test.gov"]
        "role = USER"                            | [sf("role", "USER", EQ)]                            | 2             | ["bob@example.com", "charlie@test.gov"]
        "role = ADMIN"                           | [sf("role", "ADMIN", EQ)]                           | 1             | ["alice@example.com"]
        "email contains 'example.com'"           | [sf("email", "example.com", LIKE)]                  | 2             | ["alice@example.com", "bob@example.com"]
        "email ends with 'test.gov'"             | [sf("email", "test.gov", ENDS_WITH)]                | 1             | ["charlie@test.gov"]
        "role in [ADMIN, USER]"                  | [sf("role", ["ADMIN", "USER"], IN)]                 | 3             | ["alice@example.com", "bob@example.com", "charlie@test.gov"]
        "role NOT NULL"                          | [sf("role", null, NOT_NULL)]                        | 3             | ["alice@example.com", "bob@example.com", "charlie@test.gov"]
        "tenantId IS NULL (none match here)"     | [sf("tenantId", null, IS_NULL)]                     | 0             | []   // we set tenantId="default" above
    }

    @Unroll
    def "user edge case dynamic search: #description"() {
        given:
        def pageable = PageRequest.of(0, 10)

        when:
        Page<User> page = searchService.search(User.class, filters as List, pageable)

        then:
        page.totalElements == expectedCount

        where:
        description                                   | filters                        | expectedCount
        "null filters returns all"                   | null                           | 3
        "EQ filter with null value is ignored"       | [sf("role", null, EQ)]         | 3
        "empty IN list behaves like no match"        | [sf("role", [], IN)]           | 0
    }

    private static SearchFilter sf(
            String field,
            Object value,
            SearchFilter.Operator op
    ) {
        return new SearchFilter(field, value, op)
    }
}
