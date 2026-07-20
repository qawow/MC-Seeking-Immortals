package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogTalismanServiceTest {
    @Test
    void executableTalismansClassifyAndMaterialsStayCarriers() {
        assertEquals(BulkItemKind.TALISMAN, BulkItemClassifier.classify("fire_burst_talisman", "talisman"));
        assertEquals(BulkItemKind.TALISMAN, BulkItemClassifier.classify("gold_armor_talisman", "talisman"));
        assertEquals(BulkItemKind.TALISMAN, BulkItemClassifier.classify("dingshen_fu", "talisman"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("talisman_paper", "talisman"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("recipe_binding_talisman", "talisman"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("ginseng_spirit", "talisman"));
        assertTrue(BulkItemClassifier.isExecutableTalisman("invisibility_talisman", "talisman"));
        assertFalse(BulkItemClassifier.isTalismanMaterialOrRecipe("fire_burst_talisman"));
    }

    @Test
    void roleKeywordsMapToStableModes() {
        assertEquals("aoe", CatalogTalismanService.modeKey("fire_burst_talisman", "aoe_fire"));
        assertEquals("armor", CatalogTalismanService.modeKey("gold_armor_talisman", "armor"));
        assertEquals("escape", CatalogTalismanService.modeKey("escape_heaven_talisman", "long_escape"));
        assertEquals("control", CatalogTalismanService.modeKey("binding_talisman", "bind"));
        assertEquals("invis", CatalogTalismanService.modeKey("ghost_hide_talisman", "mask_qi"));
        assertEquals("ice_projectile", CatalogTalismanService.modeKey("ice_seal_talisman", "slow_ice"));
        assertEquals("thunder", CatalogTalismanService.modeKey("thunder_talisman", "thunder_strike"));
        assertEquals("ward", CatalogTalismanService.modeKey("anti_demon_talisman", "demon_repulse"));
        assertEquals("heal", CatalogTalismanService.modeKey("life_save_talisman", "auto_resurrect_once"));
        assertEquals("contract", CatalogTalismanService.modeKey("beast_contract_talisman", "spirit_beast_contract"));
        assertEquals("utility", CatalogTalismanService.modeKey("void_palace_key_talisman", "secret_realm_hint"));
    }

    @Test
    void consumePolicyAcceptsMatchingBulkFamily() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/xunxian/seekingimmortals/skill/TalismanConsumePolicy.java"));
        assertTrue(source.contains("matchesTechnique"));
        assertTrue(source.contains("CatalogTalismanItem"));
        assertTrue(source.contains("familyOfMode"));
    }
}
