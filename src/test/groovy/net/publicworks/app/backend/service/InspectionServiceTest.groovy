// src/test/groovy/net/publicworks/app/backend/service/InspectionServiceTest.groovy
package net.publicworks.app.backend.service

import net.publicworks.app.backend.commands.AssetCommand
import net.publicworks.app.backend.commands.CrudOperation
import net.publicworks.app.backend.entity.asset.Asset
import net.publicworks.app.backend.entity.asset.AssetInspectionCommand
import net.publicworks.app.backend.entity.asset.AssetLocationCommand
import net.publicworks.app.backend.entity.asset.AssetTypeEnum
import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.repo.IAssetInspectionRepository
import net.publicworks.app.backend.repo.IAssetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Unroll

import java.time.OffsetDateTime

@SpringBootTest
@Transactional
class InspectionServiceTest extends Specification {

    @Autowired
    AssetService assetService

    @Autowired
    InspectionService inspectionService

    @Autowired
    IAssetRepository assetRepository

    @Autowired
    IAssetInspectionRepository inspectionRepository

    Asset sharedAsset

    def setup() {
        if (sharedAsset == null) {
            def locCmd = AssetLocationCommand.builder()
                    .locationType("Building")
                    .addressLine1("123 Main St")
                    .city("Schaumburg")
                    .stateProvince("IL")
                    .postalCode("60193")
                    .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                    .operation(CrudOperation.CREATE)
                    .build()
            def inspCmd = AssetInspectionCommand.builder()
                    .inspectionDate(OffsetDateTime.now())
                    .inspectorName("Initial Inspector")
                    .method("VISUAL")
                    .conditionScore(1.0)
                    .conditionClass("GOOD")
                    .notes("Initial inspection")
                    .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                    .operation(CrudOperation.CREATE)
                    .build()

            AssetCommand createCmd = AssetCommand.builder()
                    .name("Inspection Test Asset")
                    .type(AssetTypeEnum.PARKING_LOT)
                    .externalId("INSPECT-ASSET-001")
                    .status("Active")
                    .gisGeometryRef("gis-inspection-001")
                    .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                    .operation(CrudOperation.CREATE)
                    .location(locCmd)
                    .build()

            sharedAsset = assetService.create(createCmd)
        }
    }

    @Unroll
    def "create inspection #caseName for shared asset via InspectionService"() {
        given: "a fresh view of the shared asset"
        def asset = assetRepository.findById(sharedAsset.id).orElseThrow()
        int initialCount = asset.inspections?.size() ?: 0

        and: "an inspection command targeting that asset"
        def cmd = AssetInspectionCommand.builder()
                .assetId(asset.id)

                .inspectionDate(inspectionDate)
                .inspectorName(inspectorName)
                .method(method)
                .conditionScore(conditionScore)
                .conditionClass(conditionClass)
                .notes(notes)
                .attachmentGroupId(attachmentGroupId)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.CREATE)
                .build()

        when: "we create the inspection via the service"
        var saved = inspectionService.create(cmd)
        def reloadedAsset = assetRepository.findById(asset.id).orElseThrow()

        then: "the asset has one more inspection"
        reloadedAsset.inspections.size() == initialCount + 1

        and: "the latest inspection matches the command data"
        def latest = reloadedAsset.inspections.sort { it.inspectionDate }.last()
        latest.inspectionDate == inspectionDate
        latest.inspectorName == inspectorName
        latest.method == method
        latest.conditionScore == conditionScore
        latest.conditionClass == conditionClass
        latest.notes == notes
        latest.attachmentGroupId == attachmentGroupId
        latest.asset.id == asset.id

        and:
        latest.id != null


        where:
        caseName          | inspectionDate                               | inspectorName   | method   | conditionScore | conditionClass | notes                    | attachmentGroupId
        "good condition"  | OffsetDateTime.parse("2025-01-01T10:00:00Z") | "Inspector One" | "VISUAL" | 1.0            | "GOOD"         | "Everything looks good"  | "group-1"
        "fair condition"  | OffsetDateTime.parse("2025-01-02T11:00:00Z") | "Inspector Two" | "CCTV"   | 3.0            | "FAIR"         | "Some minor issues"      | "group-2"
        "poor condition"  | OffsetDateTime.parse("2025-01-03T12:00:00Z") | "Inspector 3"   | "VISUAL" | 5.0            | "POOR"         | "Significant deterioration" | "group-3"

    }

    def "modify existing inspection for shared asset via InspectionService with MODIFY operation"() {
        given: "a fresh asset and one existing inspection"
        def asset = assetRepository.findById(sharedAsset.id).orElseThrow()

        def createCmd = AssetInspectionCommand.builder()
                .assetId(asset.id)
                .inspectionDate(OffsetDateTime.parse("2025-01-10T09:00:00Z"))
                .inspectorName("Original Inspector")
                .method("VISUAL")
                .conditionScore(2.0)
                .conditionClass("FAIR")
                .notes("Initial inspection")
                .attachmentGroupId("group-original")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.CREATE)
                .build()

        inspectionService.create(createCmd)

        def afterCreate = assetRepository.findById(asset.id).orElseThrow()
        int countAfterCreate = afterCreate.inspections.size()
        assert countAfterCreate > 0

        def existing = afterCreate.inspections.sort { it.inspectionDate }.last()
        Long inspectionId = existing.id

        and: "a MODIFY command targeting the existing inspection"
        OffsetDateTime newDate = OffsetDateTime.parse("2025-02-01T15:30:00Z")

        def modifyCmd = AssetInspectionCommand.builder()
                .id(inspectionId)                     // 👈 important for updateEntitiesById
                .assetId(asset.id)
                .inspectionDate(newDate)
                .inspectorName("Modified Inspector")
                .method("CCTV")
                .conditionScore(4.5)
                .conditionClass("POOR")
                .notes("Updated findings after follow-up")
                .attachmentGroupId("group-modified")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.MODIFY)      // 👈 op = MODIFY (even if logic only keys off id)
                .build()

        when: "we send the modify command through the same service"
        inspectionService.create(modifyCmd)           // same entry point, different operation
        def reloaded = assetRepository.findById(asset.id).orElseThrow()

        then: "we still have the same number of inspections (no duplicate created)"
        reloaded.inspections.size() == countAfterCreate

        and: "the targeted inspection was updated in-place"
        def modified = reloaded.inspections.find { it.id == inspectionId }
        modified != null
        modified.inspectionDate == newDate
        modified.inspectorName == "Modified Inspector"
        modified.method == "CCTV"
        modified.conditionScore == 4.5d
        modified.conditionClass == "POOR"
        modified.notes == "Updated findings after follow-up"
        modified.attachmentGroupId == "group-modified"
        modified.asset.id == asset.id
    }

    def "delete existing inspection for shared asset via InspectionService with DELETE operation"() {
        given: "a fresh asset and one existing inspection"
        def asset = assetRepository.findById(sharedAsset.id).orElseThrow()

        def createCmd = AssetInspectionCommand.builder()
                .assetId(asset.id)
                .inspectionDate(OffsetDateTime.parse("2025-01-10T09:00:00Z"))
                .inspectorName("Original Inspector")
                .method("VISUAL")
                .conditionScore(2.0)
                .conditionClass("FAIR")
                .notes("Initial inspection")
                .attachmentGroupId("group-original")
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.CREATE)
                .build()

        inspectionService.create(createCmd)

        def afterCreate = assetRepository.findById(asset.id).orElseThrow()
        int countAfterCreate = afterCreate.inspections.size()
        assert countAfterCreate > 0

        def existing = afterCreate.inspections.sort { it.inspectionDate }.last()
        Long inspectionId = existing.id

        and: "a DELETE command targeting that inspection"
        def deleteCmd = AssetInspectionCommand.builder()
                .id(inspectionId)
                .assetId(asset.id)
                .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                .operation(CrudOperation.DELETE)
                .build()

        when: "we send the delete command through the same service"
        inspectionService.delete(deleteCmd)
        def reloaded = assetRepository.findById(asset.id).orElseThrow()

        then: "the inspection count decreased by one"
        reloaded.inspections.size() == countAfterCreate - 1

        and: "the targeted inspection is gone"
        reloaded.inspections.find { it.id == inspectionId } == null
        var inspection = inspectionRepository.findById(inspectionId)
        inspection.isEmpty()
    }
}
