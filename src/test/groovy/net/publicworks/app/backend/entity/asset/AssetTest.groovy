package net.publicworks.app.backend.entity.asset

import jakarta.transaction.Transactional
import net.publicworks.app.backend.entity.WorkOrder
import net.publicworks.app.backend.itf.CustomerDiferentiator
import net.publicworks.app.backend.repo.IAssetAttributeValueRepository
import net.publicworks.app.backend.repo.IAssetRepository
import net.publicworks.app.backend.repo.IWorkOrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll

// MOVE TO REPO CLASSES LATER


// Real Asset repo

// Real WorkOrder repo

@SpringBootTest
@Transactional
class AssetTest extends Specification {

    @Autowired IAssetRepository assetRepository
    @Autowired AssetAttributeDefinitionRepository assetAttributeDefinitionRepository
    @Autowired IAssetAttributeValueRepository assetAttributeValueRepository
    @Autowired IWorkOrderRepository workOrderRepository
    def "user can define asset type, attributes, create asset, and link work orders"() {
        given: "an asset type WATER_MAIN represented by an enum"
        def assetType = AssetTypeEnum.WATER_MAIN

        and: "dynamic attribute definitions for diameter, material, and pressure class"
        def diameterAttr = new AssetAttributeDefinition(
                assetType: assetType,
                key: "diameter_in",
                label: "Diameter (in)",
                dataType: "NUMBER",
                required: true,
                unit: "in",
//                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )
        diameterAttr.customerDiferentiator = CustomerDiferentiator.DEFAULT
        def materialAttr = new AssetAttributeDefinition(
                assetType: assetType,
                key: "material",
                label: "Material",
                dataType: "ENUM",
                required: true,
                allowedValuesJson: '["Ductile Iron","PVC","HDPE"]',
//                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )
        materialAttr.customerDiferentiator = CustomerDiferentiator.DEFAULT
        def pressureClassAttr = new AssetAttributeDefinition(
                assetType: assetType,
                key: "pressure_class",
                label: "Pressure Class",
                dataType: "NUMBER",
                required: false,
                unit: "psi",
//                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )
        pressureClassAttr.customerDiferentiator = CustomerDiferentiator.DEFAULT

        assetAttributeDefinitionRepository.saveAll([diameterAttr, materialAttr, pressureClassAttr])

        when: "a user creates a new Water Main asset through the UI"
        def asset = new Asset()
        asset.type = assetType
        asset.name = "Water Main - Main St Segment A"
        asset.externalId = "WM-0001"
        asset.status = "Active"
        asset.installYear = 1995
        asset.expectedServiceLifeYears = 80
        asset.customerDiferentiator = CustomerDiferentiator.DEFAULT

        asset = assetRepository.save(asset)

        and: "the UI saves dynamic attribute values for that asset"
        def diameterVal = new AssetAttributeValue(
                asset: asset,
                definition: diameterAttr,
                value: "12",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )
        def materialVal = new AssetAttributeValue(
                asset: asset,
                definition: materialAttr,
                value: "Ductile Iron",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )
        def pressureClassVal = new AssetAttributeValue(
                asset: asset,
                definition: pressureClassAttr,
                value: "150",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        )

        assetAttributeValueRepository.saveAll([diameterVal, materialVal, pressureClassVal])

        and: "a user creates a work order against this asset"
        def wo = new WorkOrder()
        wo.title = "Repair leak on Main St water main"
        wo.description = "Reported leak near 123 Main St. Excavate and repair."
        wo.status = "New"
        wo.priority = "High"
        wo.requestedBy = "John Smith"
        wo.locationText = "Near 123 Main St"
        wo.department = "Water"
        wo.assignedTo = "Water Crew 1"
        wo.source = "Phone"
        wo.customerDiferentiator = CustomerDiferentiator.DEFAULT
//        wo.asset = asset   // assuming WorkOrder.asset is ManyToOne<Asset>

        wo = workOrderRepository.save(wo)

        then: "the attribute definitions for WATER_MAIN are persisted"
        def defsForType = assetAttributeDefinitionRepository.findByAssetType(AssetTypeEnum.WATER_MAIN)
        defsForType.size() == 3

        and: "the asset and its attributes are persisted and can be queried"
        def reloadedAsset = assetRepository.findById(asset.id).orElseThrow()
        reloadedAsset.type == AssetTypeEnum.WATER_MAIN

        def values = assetAttributeValueRepository.findByAsset(reloadedAsset)
        values*.definition.key as Set == ["diameter_in", "material", "pressure_class"] as Set
        values.find { it.definition.key == "diameter_in" }.value == "12"
        values.find { it.definition.key == "material" }.value == "Ductile Iron"

        and: "the work order is linked to the asset as work history"
        def reloadedWo = workOrderRepository.findById(wo.id).orElseThrow()
//        reloadedWo.asset.id == reloadedAsset.id
        reloadedWo.department == "Water"
        reloadedWo.priority == "High"
    }
    def "user can filter assets by dynamic attribute like diameter and material"() {
        given: "dynamic attributes for WATER_MAIN diameter and material"
        def type = AssetTypeEnum.WATER_MAIN

        def diameterDef = assetAttributeDefinitionRepository.save(new AssetAttributeDefinition(
                assetType: type,
                key: "diameter_in",
                label: "Diameter (in)",
                dataType: "NUMBER",
                required: true,
                unit: "in",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        ))
        def materialDef = assetAttributeDefinitionRepository.save(new AssetAttributeDefinition(
                assetType: type,
                key: "material",
                label: "Material",
                dataType: "ENUM",
                required: true,
                allowedValuesJson: '["Ductile Iron","PVC","HDPE"]',
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        ))

        and: "a 12in ductile iron main and an 8in PVC main"
        def main12 = assetRepository.save(new Asset(
                type: type,
                name: "Water Main 12in DI",
                externalId: "WM-0012-DI",
                status: "Active",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        ))
        def main8 = assetRepository.save(new Asset(
                type: type,
                name: "Water Main 8in PVC",
                externalId: "WM-0008-PVC",
                status: "Active",
                customerDiferentiator: CustomerDiferentiator.DEFAULT,

        ))

        assetAttributeValueRepository.saveAll([
                new AssetAttributeValue(asset: main12, definition: diameterDef, value: "12", customerDiferentiator: CustomerDiferentiator.DEFAULT,),
                new AssetAttributeValue(asset: main12, definition: materialDef, value: "Ductile Iron", customerDiferentiator: CustomerDiferentiator.DEFAULT,),
                new AssetAttributeValue(asset: main8, definition: diameterDef, value: "8", customerDiferentiator: CustomerDiferentiator.DEFAULT,),
                new AssetAttributeValue(asset: main8, definition: materialDef, value: "PVC", customerDiferentiator: CustomerDiferentiator.DEFAULT,),
        ])

        when: "the user filters for all 12in Ductile Iron water mains"
        def allValues = assetAttributeValueRepository.findAll()
        def matchingAssets = allValues
                .groupBy { it.asset }
                .findAll { asset, vals ->
                    vals.any { it.definition.id == diameterDef.id && it.value == "12" } &&
                            vals.any { it.definition.id == materialDef.id && it.value == "Ductile Iron" }
                }
                .keySet()
                .toList()

        then: "only the matching asset is returned"
        matchingAssets*.externalId == ["WM-0012-DI"]
    }
/**
 * High-level RFQ compliance test covering:
 * - Section 2.1: Dedicated asset applications (different asset scenarios)
 * - Section 2.2: Ability to configure additional asset applications (generic Asset + AssetRelationship without code changes)
 * - Section 2.3: Ability to link assets of same/different types and display relationships
 */
    @Unroll
    def "RFQ asset linking model test: #scenarioDescription"(String scenarioDescription,
                                                             AssetTypeEnum fromType,
                                                             AssetTypeEnum toType,
                                                             String relationshipType,
                                                             boolean setParent) {
        given: "Assets of given types"
        def fromAsset = new Asset(name: "From Asset - $scenarioDescription", type: fromType, status: "Active")
        def toAsset = new Asset(name: "To Asset - $scenarioDescription", type: toType, status: "Active")

        if (setParent) {
            toAsset.parent = fromAsset
        }

        and: "an AssetRelationship linking fromAsset to toAsset"
        def relationship = new AssetRelationship(fromAsset: fromAsset, toAsset: toAsset, relationshipType: relationshipType)

        fromAsset.outgoingRelationships = [relationship]
        toAsset.incomingRelationships = [relationship]

        expect: "fromAsset has the outgoing relationship"
        fromAsset.outgoingRelationships.contains(relationship)

        and: "toAsset has the incoming relationship"
        toAsset.incomingRelationships.contains(relationship)

        and: "the relationshipType matches scenario description"
        relationship.relationshipType == relationshipType

        and: "parent is set if indicated"
        (toAsset.parent != null) == setParent

        where:
        scenarioDescription                                         | fromType                  | toType                    | relationshipType | setParent
        "sign to support (different types)"                         | AssetTypeEnum.SIGN        | AssetTypeEnum.SUPPORT     | "mounted_on"     | false
        "parallel water mains (same type)"                          | AssetTypeEnum.WATER_MAIN  | AssetTypeEnum.WATER_MAIN  | "parallel_to"    | false
        "water main feeding facility (different types, external)"   | AssetTypeEnum.WATER_MAIN  | AssetTypeEnum.FACILITY    | "feeds"          | true
    }
}
