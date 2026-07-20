package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogEquipmentServiceTest {
    @Test
    void equipmentClassifiesAndModesResolve() {
        assertEquals(BulkItemKind.EQUIPMENT, BulkItemClassifier.classify("spirit_boat_low", "equipment"));
        assertEquals(BulkItemKind.EQUIPMENT, BulkItemClassifier.classify("basic_wood_puppet", "equipment"));
        assertEquals(BulkItemKind.EQUIPMENT, BulkItemClassifier.classify("alchemy_furnace_g2", "equipment"));
        assertEquals("vehicle", CatalogEquipmentService.modeKey("spirit_boat_mid"));
        assertEquals("puppet", CatalogEquipmentService.modeKey("giant_ape_puppet"));
        assertEquals("furnace", CatalogEquipmentService.modeKey("alchemy_furnace_g3"));
    }

    @Test
    void formationBehaviorsNoLongerInspectOnly() throws Exception {
        String json = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/data/seeking_immortals/text_material/formation_item_behaviors.json"));
        assertFalse(json.contains("\"action\": \"inspect_only\"") || json.contains("\"action\":\"inspect_only\""));
        assertTrue(json.contains("nine_palace_disk"));
        assertTrue(json.contains("kill_array_core"));
        String service = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/structure/FormationItemService.java"));
        assertTrue(service.contains("Legacy inspect_only is treated as free-field activation"));
        assertFalse(service.contains("activate_free_field\").equals(action) && behavior.uses() == null"));
    }
}
