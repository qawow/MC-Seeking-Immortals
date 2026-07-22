package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 0.2.109: structure_repair_bench / structure_blueprint_table must route through StructureToolService.
 */
class StructureToolServiceTest {

    @Test
    void metaToolServiceHandlesRepairAndBlueprintIds() throws Exception {
        String service = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "StructureToolService.java"));
        assertTrue(service.contains("case \"structure_repair_bench\""));
        assertTrue(service.contains("case \"structure_blueprint_table\""));
        assertTrue(service.contains("MultiblockOperationalService.inspect"));
        assertTrue(service.contains("MultiblockOperationalService.repair"));
        assertTrue(service.contains("MultiblockOperationalService.form"));
        assertTrue(service.contains("MultiblockSequenceDisplayCatalog"));
    }

    @Test
    void baseMaterialItemDispatchesStructureTools() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "material", "BaseMaterialItem.java"));
        assertTrue(source.contains("StructureToolService.tryUse"));
        int formation = source.indexOf("FormationItemService.tryUse");
        int structure = source.indexOf("StructureToolService.tryUse");
        assertTrue(formation >= 0 && structure > formation,
                "structure tools should run after formation item dispatch");
        int useOn = source.indexOf("InteractionResult useOn(UseOnContext context)");
        int structureUseOn = source.indexOf("StructureToolService", useOn);
        assertTrue(useOn >= 0 && structureUseOn > useOn,
                "structure tools must also dispatch from the block-use path");
    }

    @Test
    void metaToolsAreNotStructureTokenCarriers() {
        assertFalse(com.xunxian.seekingimmortals.item.CatalogItemDescriptionService
                .isStructureTokenCarrier("structure_repair_bench"));
        assertFalse(com.xunxian.seekingimmortals.item.CatalogItemDescriptionService
                .isStructureTokenCarrier("structure_blueprint_table"));
        assertTrue(com.xunxian.seekingimmortals.item.CatalogItemDescriptionService
                .isStructureTokenCarrier("kill_array_hub"));
    }
}
