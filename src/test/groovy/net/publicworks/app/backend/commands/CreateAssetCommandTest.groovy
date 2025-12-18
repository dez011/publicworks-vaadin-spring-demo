// src/test/groovy/net/publicworks/app/backend/commands/CreateAssetCommandTest.groovy
package net.publicworks.app.backend.commands

import net.publicworks.app.backend.entity.asset.Asset
import net.publicworks.app.backend.entity.asset.AssetLocation
import net.publicworks.app.backend.entity.asset.AssetLocationCommand
import net.publicworks.app.backend.entity.asset.AssetTypeEnum
import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.repo.IAssetRepository
import net.publicworks.app.backend.repo.ILocationRepository
import net.publicworks.app.backend.service.AssetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@Transactional
class CreateAssetCommandTest extends Specification {

    @Autowired
    AssetService assetService

    @Autowired
    ILocationRepository locationRepository

    @Autowired
    IAssetRepository assetRepository                             // 👈 add this

    def "create an asset with new location"() {
        when:
        def locCmd = AssetLocationCommand.builder()
                .locationType("Building")
                .addressLine1("123 Main St")
                .city("Schaumburg")
                .stateProvince("IL")
                .postalCode("60193")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.CREATE)
                .build()

        AssetCommand createCmd = AssetCommand.builder()
                .name("Test Asset")
                .type(AssetTypeEnum.PARKING_LOT)
                .externalId("PL-001")
                .status("Active")
                .gisGeometryRef("gis-001")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        Asset created = assetService.create(createCmd)

        then:
        created.id != null
        created.name == "Test Asset"
        created.location != null
        created.location.id != null
        created.location.locationType == "Building"
        created.location.addressLine1 == "123 Main St"
    }

    def "create an asset reusing existing location"() {
        given: "an existing persisted location"
        def existingLoc = new AssetLocation()
        existingLoc.locationType = "EXISTING"
        existingLoc.addressLine1 = "999 Old Rd"
        existingLoc.city = "Schaumburg"
        existingLoc.stateProvince = "IL"
        existingLoc.postalCode = "60193"
        existingLoc.customerDiferentiator = CustomerDiferentiator.DEFAULT
        existingLoc = locationRepository.save(existingLoc)

        when: "we create an asset that references the existing location id"
        def locCmd = AssetLocationCommand.builder()
                .id(existingLoc.id)
                .locationType(existingLoc.locationType)
                .addressLine1(existingLoc.addressLine1)
                .city(existingLoc.city)
                .stateProvince(existingLoc.stateProvince)
                .postalCode(existingLoc.postalCode)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .build()

        AssetCommand createCmd = AssetCommand.builder()
                .name("Asset With Existing Location")
                .type(AssetTypeEnum.PARKING_LOT)
                .externalId("PL-EXIST")
                .status("Active")
                .gisGeometryRef("gis-002")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        Asset created = assetService.create(createCmd)

        then: "the asset uses the same location row (no new location is created)"
        created.id != null
        created.location != null
        created.location.id == existingLoc.id
        created.location.addressLine1 == "999 Old Rd"
    }

    def "multiple assets share same location and location links back to all assets"() {
        given: "an existing persisted location"
        def existingLoc = new AssetLocation()
        existingLoc.locationType = "SHARED"
        existingLoc.addressLine1 = "777 Shared Rd"
        existingLoc.city = "Schaumburg"
        existingLoc.stateProvince = "IL"
        existingLoc.postalCode = "60193"
        existingLoc.customerDiferentiator = CustomerDiferentiator.DEFAULT
        existingLoc = locationRepository.save(existingLoc)

        and: "three asset commands that all reference the same location"
        def locCmd = AssetLocationCommand.builder()
                .id(existingLoc.id)
                .locationType(existingLoc.locationType)
                .addressLine1(existingLoc.addressLine1)
                .city(existingLoc.city)
                .stateProvince(existingLoc.stateProvince)
                .postalCode(existingLoc.postalCode)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .build()

        AssetCommand createCmd1 = AssetCommand.builder()
                .name("Shared Asset 1")
                .type(AssetTypeEnum.PARKING_LOT)
                .externalId("PL-SHARED-1")
                .status("Active")
                .gisGeometryRef("gis-s-1")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        AssetCommand createCmd2 = AssetCommand.builder()
                .name("Shared Asset 2")
                .type(AssetTypeEnum.PARKING_LOT)
                .externalId("PL-SHARED-2")
                .status("Active")
                .gisGeometryRef("gis-s-2")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        AssetCommand createCmd3 = AssetCommand.builder()
                .name("Shared Asset 3")
                .type(AssetTypeEnum.PARKING_LOT)
                .externalId("PL-SHARED-3")
                .status("Active")
                .gisGeometryRef("gis-s-3")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        when: "we create three assets that reuse the same location"
        Asset asset1 = assetService.create(createCmd1)
        Asset asset2 = assetService.create(createCmd2)
        Asset asset3 = assetService.create(createCmd3)

        and: "we reload the location from the repository"
        def reloadedLoc = locationRepository.findById(existingLoc.id).orElseThrow()

        then: "all three assets share the same location id"
        asset1.location.id == existingLoc.id
        asset2.location.id == existingLoc.id
        asset3.location.id == existingLoc.id

        and: "the location links back to all three assets"
        reloadedLoc.assets*.id.containsAll([asset1.id, asset2.id, asset3.id])
        reloadedLoc.assets.size() == 3
    }

    def "removing location from asset detaches relationship on both sides"() {
        given: "a persisted location and asset linked to it"
        def loc = new AssetLocation()
        loc.locationType = "DETACH_TEST"
        loc.addressLine1 = "100 Detach St"
        loc.city = "Schaumburg"
        loc.stateProvince = "IL"
        loc.postalCode = "60193"
        loc.customerDiferentiator = CustomerDiferentiator.DEFAULT
        loc = locationRepository.save(loc)

        def asset = new Asset()
        asset.customerDiferentiator = CustomerDiferentiator.DEFAULT
        asset.name = "Asset Detach"
        asset.type = AssetTypeEnum.PARKING_LOT
        asset.status = "Active"
        asset.setLocation(loc)
        asset = assetRepository.save(asset)

        when: "we remove the location from the asset"
        asset.setLocation(null)
        asset = assetRepository.save(asset)
        def reloadedAsset = assetRepository.findById(asset.id).orElseThrow()
        def reloadedLoc = locationRepository.findById(loc.id).orElseThrow()

        then: "the asset no longer has a location"
        reloadedAsset.location == null

        and: "the location no longer references the asset"
        !reloadedLoc.assets*.id.contains(asset.id)
    }

    def "removing location via command detaches relationship on both sides"() {
        given: "an asset with an existing location"
        def loc = new AssetLocation()
        loc.locationType = "DETACH_CMD"
        loc.addressLine1 = "200 Cmd St"
        loc.city = "Schaumburg"
        loc.stateProvince = "IL"
        loc.postalCode = "60193"
        loc.customerDiferentiator = CustomerDiferentiator.DEFAULT
        loc = locationRepository.save(loc)

        def asset = new Asset()
        asset.customerDiferentiator = CustomerDiferentiator.DEFAULT
        asset.name = "Asset Cmd Detach"
        asset.type = AssetTypeEnum.PARKING_LOT
        asset.status = "Active"
        asset.setLocation(loc)
        asset = assetRepository.save(asset)

        and: "we build a command that says 'REMOVE' for location"
        def locCmd = AssetLocationCommand.builder()
                .operation(CrudOperation.REMOVE)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .build()

        def modifyCmd = AssetCommand.builder()
                .id(asset.id)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .location(locCmd)
                .build()

        when: "we apply the command through the service (which calls Asset.apply)"
        assetService.modify(modifyCmd)  // whatever your modify handler is called

        and: "reload from DB"
        def reloadedAsset = assetRepository.findById(asset.id).orElseThrow()
        def reloadedLoc = locationRepository.findById(loc.id).orElseThrow()

        then: "the asset no longer has a location"
        reloadedAsset.location == null

        and: "the location no longer references the asset"
        !reloadedLoc.assets*.id.contains(asset.id)
    }


    def "modifying asset and existing location via command updates both"() {
        given: "an asset with an existing location"
        def loc = new AssetLocation()
        loc.locationType = "ORIGINAL"
        loc.addressLine1 = "10 Old St"
        loc.city = "Schaumburg"
        loc.stateProvince = "IL"
        loc.postalCode = "60193"
        loc.customerDiferentiator = CustomerDiferentiator.DEFAULT
        loc = locationRepository.save(loc)

        def asset = new Asset()
        asset.customerDiferentiator = CustomerDiferentiator.DEFAULT
        asset.name = "Original Asset Name"
        asset.type = AssetTypeEnum.PARKING_LOT
        asset.status = "Active"
        asset.gisGeometryRef = "gis-old"
        asset.installYear = 2000
        asset.expectedServiceLifeYears = 20
        asset.replacementCostEstimate = 1000D
        asset.setLocation(loc)
        asset = assetRepository.save(asset)

        and: "a location command that modifies the existing location"
        def locCmd = AssetLocationCommand.builder()
                .id(loc.id)                          // same row
                .operation(CrudOperation.MODIFY)     // drive modify semantics
                .addressLine1("20 New St")
                .city("Hoffman Estates")
                .stateProvince("IL")
                .postalCode("60169")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .build()

        and: "an asset command that modifies asset fields and includes the location command"
        def modifyCmd = AssetCommand.builder()
                .id(asset.id)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .name("Updated Asset Name")
                .status("Out of Service")
                .gisGeometryRef("gis-new")
                .installYear(2010)
                .expectedServiceLifeYears(30)
                .replacementCostEstimate(2500D)
                .location(locCmd)
                .build()

        when: "we apply the command through the service (which calls Asset.apply + resolveChild)"
        assetService.modify(modifyCmd)

        and: "reload from DB"
        def reloadedAsset = assetRepository.findById(asset.id).orElseThrow()
        def reloadedLoc = locationRepository.findById(loc.id).orElseThrow()

        then: "asset fields are updated"
        reloadedAsset.name == "Updated Asset Name"
        reloadedAsset.status == "Out of Service"
        reloadedAsset.gisGeometryRef == "gis-new"
        reloadedAsset.installYear == 2010
        reloadedAsset.expectedServiceLifeYears == 30
        reloadedAsset.replacementCostEstimate == 2500D

        and: "asset still points to the same location row"
        reloadedAsset.location != null
        reloadedAsset.location.id == loc.id

        and: "location fields are updated, not replaced with a new row"
        reloadedAsset.location.addressLine1 == "20 New St"
        reloadedAsset.location.city == "Hoffman Estates"
        reloadedAsset.location.postalCode == "60169"

        and: "the location still links back to the asset"
        reloadedLoc.assets*.id.contains(reloadedAsset.id)
    }
}
