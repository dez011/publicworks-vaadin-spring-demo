package net.publicworks.app.backend.repo

import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.repo.hibernateMultiTenantConfig.MultiTenantConnectionProviderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import java.sql.ResultSet

@SpringBootTest
@ActiveProfiles("dev") // or whatever profile you use for H2/dev
class MultiTenantConnectionProviderImplSpec extends Specification {

    @Autowired
    MultiTenantConnectionProviderImpl multiTenantConnectionProvider

    def "print DB URL and tables for each known tenant"() {
        when: "we inspect all tenants we care about"
        def tenants = [CustomerDiferentiator.DEFAULT, "alaska"]  // add more tenant IDs here

        tenants.each { tenantId ->
            def ds = multiTenantConnectionProvider.getDataSourceForTenant(tenantId)
            assert ds != null : "No DataSource registered for tenant: $tenantId"

            ds.connection.withCloseable { conn ->
                def meta = conn.metaData
                println "====== Tenant: $tenantId ======"
                println "JDBC URL: ${meta.URL}"
                println "User: ${meta.userName}"

                // H2/Postgres: list tables
                ResultSet rs = meta.getTables(null, null, "%", ["TABLE"] as String[])
                def tableNames = [] as List<String>
                while (rs.next()) {
                    tableNames << rs.getString("TABLE_NAME")
                }
                rs.close()

                println "Tables: ${tableNames.sort()}"
            }
        }

        then:
        // this is more of an inspection test – just assert it didn't explode
        noExceptionThrown()
    }
}
