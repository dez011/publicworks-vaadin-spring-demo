package net.publicworks.app.backend.service

import net.publicworks.app.backend.service.search.GenericSearchCommand
import net.publicworks.app.backend.service.search.SearchHandler

import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.service.search.SearchFilter
import net.publicworks.app.backend.service.search.SearchService
import net.publicworks.app.VaadinApplication
import net.publicworks.app.backend.entity.WorkOrder
import net.publicworks.app.backend.repo.IWorkOrderRepository
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
class SearchServiceImplTest extends Specification {
        @Autowired
        SearchService searchService

        @Autowired
        IWorkOrderRepository workOrderRepo

        @Autowired
        SearchHandler searchHandler

        def setup() {
            // make sure we start clean
            workOrderRepo.deleteAll()

            // seed some test data
            workOrderRepo.saveAll([
                    new WorkOrder().tap {
                        title = "Water main break on 5th"
                        description = "Major leak near school"
                        status = "OPEN"
                        priority = "HIGH"
                        customerDiferentiator = CustomerDiferentiator.DEFAULT
                    },
                    new WorkOrder().tap {
                        title = "Street light out"
                        description = "Lamp post near park"
                        status = "OPEN"
                        priority = "LOW"
                        customerDiferentiator = CustomerDiferentiator.DEFAULT
                    },
                    new WorkOrder().tap {
                        title = "Pothole on Elm"
                        description = "Large pothole"
                        status = "CLOSED"
                        priority = "MEDIUM"
                        customerDiferentiator = CustomerDiferentiator.DEFAULT
                    }
            ])
        }

        def "search by text and status filters correctly"() {
            given: "filters with text 'leak' and status OPEN"
            def filters = [
                    new SearchFilter(
                            "description",
                            "leak",
                            SearchFilter.Operator.LIKE
                    ),
                    new SearchFilter(
                            "status",
                            "OPEN",
                            SearchFilter.Operator.EQ
                    )
            ]

            and: "a simple pageable"
            def pageable = PageRequest.of(0, 10)

            when:
            Page<WorkOrder> page = searchService.search(WorkOrder.class, filters, pageable)

            then:
            page.totalElements == 1
            page.content[0].title == "Water main break on 5th"
        }

        def "search by priority only"() {
            given:
            def filters = [
                    new SearchFilter(
                            "priority",
                            "LOW",
                            SearchFilter.Operator.EQ
                    )
            ]
            def pageable = PageRequest.of(0, 10)

            when:
            def page = searchService.search(WorkOrder.class, filters, pageable)

            then:
            page.totalElements == 1
            page.content[0].title == "Street light out"
        }

        def "empty params returns all work orders"() {
            given:
            def filters = []
            def pageable = PageRequest.of(0, 10)

            when:
            def page = searchService.search(WorkOrder.class, filters, pageable)

            then:
            page.totalElements == 3
        }

        @Unroll
        def "dynamic search scenario: #description"() {
            given:
            def pageable = PageRequest.of(0, 10)

            when:
            Page<WorkOrder> page = searchService.search(WorkOrder.class, filters as List, pageable)

            then:
            page.totalElements == expectedCount
            (page.content*.title as Set) == (expectedTitles as Set)

            where:
            description                          | filters                                                                                  | expectedCount | expectedTitles
            "no filters returns all"           | []                                                                                       | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "status = OPEN"                    | [sf("status", "OPEN", EQ)]                                                               | 2             | ["Water main break on 5th", "Street light out"]
            "status = CLOSED"                  | [sf("status", "CLOSED", EQ)]                                                             | 1             | ["Pothole on Elm"]
            "priority = LOW"                   | [sf("priority", "LOW", EQ)]                                                              | 1             | ["Street light out"]
            "description contains 'leak'"      | [sf("description", "leak", LIKE)]                                                        | 1             | ["Water main break on 5th"]
            "title starts with 'Street'"       | [sf("title", "Street", STARTS_WITH)]                                                     | 1             | ["Street light out"]
            "title ends with 'on Elm'"         | [sf("title", "on Elm", ENDS_WITH)]                                                       | 1             | ["Pothole on Elm"]
            "status in [OPEN, CLOSED]"         | [sf("status", ["OPEN", "CLOSED"], IN)]                                                   | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "status NOT NULL"                  | [sf("status", null, NOT_NULL)]                                                           | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "requesterEmail IS NULL"           | [sf("requesterEmail", null, IS_NULL)]                                                    | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
        }

        @Unroll
        def "edge case dynamic search: #description"() {
            given:
            def pageable = PageRequest.of(0, 10)

            when:
            Page<WorkOrder> page = searchService.search(WorkOrder.class, filters as List, pageable)

            then:
            page.totalElements == expectedCount

            where:
            description                              | filters                                     | expectedCount
            "null filters returns all"             | null                                        | 3
            "EQ filter with null value is ignored" | [sf("status", null, EQ)]                    | 3
            "empty IN list behaves like no match"  | [sf("status", [], IN)]                      | 0
        }


        @Unroll
        def "generic search handler dynamic scenario: #description"() {
            given:
            def cmd = new GenericSearchCommand<>(WorkOrder)
            cmd.setPage(0)
            cmd.setSize(10)
            cmd.setSortBy("id")

            // copy filters from the where: table into the command
            (filters as List)?.each { SearchFilter f ->
                cmd.addFilter(f)
            }

            when:
            def result = searchHandler.handle(cmd)
            Page<WorkOrder> page = (Page<WorkOrder>) result.data

            then:
            page.totalElements == expectedCount
            (page.content*.title as Set) == (expectedTitles as Set)

            where:
            description                          | filters                                                                                  | expectedCount | expectedTitles
            "no filters returns all"           | []                                                                                       | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "status = OPEN"                    | [sf("status", "OPEN", EQ)]                                                               | 2             | ["Water main break on 5th", "Street light out"]
            "status = CLOSED"                  | [sf("status", "CLOSED", EQ)]                                                             | 1             | ["Pothole on Elm"]
            "priority = LOW"                   | [sf("priority", "LOW", EQ)]                                                              | 1             | ["Street light out"]
            "description contains 'leak'"      | [sf("description", "leak", LIKE)]                                                        | 1             | ["Water main break on 5th"]
            "title starts with 'Street'"       | [sf("title", "Street", STARTS_WITH)]                                                     | 1             | ["Street light out"]
            "title ends with 'on Elm'"         | [sf("title", "on Elm", ENDS_WITH)]                                                       | 1             | ["Pothole on Elm"]
            "status in [OPEN, CLOSED]"         | [sf("status", ["OPEN", "CLOSED"], IN)]                                                   | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "status NOT NULL"                  | [sf("status", null, NOT_NULL)]                                                           | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
            "requesterEmail IS NULL"           | [sf("requesterEmail", null, IS_NULL)]                                                    | 3             | ["Water main break on 5th", "Street light out", "Pothole on Elm"]
        }

        @Unroll
        def "generic search handler edge cases: #description"() {
            given:
            def cmd = new GenericSearchCommand<>(WorkOrder)
            cmd.setPage(0)
            cmd.setSize(10)
            cmd.setSortBy("id")

            (filters as List)?.each { SearchFilter f ->
                cmd.addFilter(f)
            }

            when:
            def result = searchHandler.handle(cmd)
            Page<WorkOrder> page = (Page<WorkOrder>) result.data

            then:
            page.totalElements == expectedCount

            where:
            description                              | filters                                     | expectedCount
            "null filters returns all"             | null                                        | 3
            "EQ filter with null value is ignored" | [sf("status", null, EQ)]                    | 3
        }

        private static SearchFilter sf(String field, Object value, SearchFilter.Operator op) {
            return new SearchFilter(field, value, op)
        }
}
