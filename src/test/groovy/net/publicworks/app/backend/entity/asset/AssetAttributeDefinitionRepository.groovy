package net.publicworks.app.backend.entity.asset

import org.springframework.data.jpa.repository.JpaRepository

public interface AssetAttributeDefinitionRepository extends JpaRepository<AssetAttributeDefinition, Long> {
    // Attribute definitions are keyed by AssetTypeEnum now
    List<AssetAttributeDefinition> findByAssetType(AssetTypeEnum type);
}
