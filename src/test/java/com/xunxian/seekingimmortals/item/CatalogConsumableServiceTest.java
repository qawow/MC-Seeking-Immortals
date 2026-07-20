package com.xunxian.seekingimmortals.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogConsumableServiceTest {
    @Test
    void lightningWardOnlyAppliesToLightningOrTribulationMarkers() {
        assertTrue(CatalogConsumableService.isLightningStrike(true, false));
        assertTrue(CatalogConsumableService.isLightningStrike(false, true));
        assertFalse(CatalogConsumableService.isLightningStrike(false, false));
        assertEquals(0.35F,
                CatalogConsumableService.lightningDamageMultiplier(1, true), 0.0001F);
        assertEquals(1.0F,
                CatalogConsumableService.lightningDamageMultiplier(0, true), 0.0001F);
        assertEquals(1.0F,
                CatalogConsumableService.lightningDamageMultiplier(3, false), 0.0001F);
    }

    @Test
    void storageDefinitionsRemainPortableAndNonConsumable() {
        var low = com.xunxian.seekingimmortals.registry.BulkItemClassifier
                .consumable("storage_pouch_low").orElseThrow();
        var mid = com.xunxian.seekingimmortals.registry.BulkItemClassifier
                .consumable("storage_pouch_mid").orElseThrow();
        var high = com.xunxian.seekingimmortals.registry.BulkItemClassifier
                .consumable("storage_bag_high").orElseThrow();
        assertEquals(9, low.storageSlots());
        assertEquals("portable_storage_9", low.effect());
        assertEquals(18, mid.storageSlots());
        assertEquals("portable_storage_18", mid.effect());
        assertEquals(27, high.storageSlots());
        assertEquals("portable_storage_27", high.effect());
    }

    @Test
    void deferredCraftMaterialsAreNotConsumedByRightClick() {
        assertFalse(CatalogConsumableService.shouldConsumeOnSuccess("talisman_craft_material", 0));
        assertFalse(CatalogConsumableService.shouldConsumeOnSuccess("array_fuel", 0));
        assertFalse(CatalogConsumableService.shouldConsumeOnSuccess("portable_storage_9", 9));
        assertTrue(CatalogConsumableService.shouldConsumeOnSuccess("detox_minor", 0));
        assertTrue(CatalogConsumableService.shouldConsumeOnSuccess("corpse_control", 0));
        assertTrue(CatalogConsumableService.shouldConsumeOnSuccess("vehicle_craft", 0));
        assertTrue(CatalogConsumableService.shouldConsumeOnSuccess("sect_contribution_redeem", 0));
    }

    @Test
    void remainingCatalogConsumablesResolveExecutableEffects() {
        var cases = java.util.Map.ofEntries(
                java.util.Map.entry("spirit_boat_ticket", "travel_spirit_boat"),
                java.util.Map.entry("ferry_pass", "travel_nether_ferry"),
                java.util.Map.entry("teleport_talisman_chaotic_sea", "travel_chaotic_sea"),
                java.util.Map.entry("diyuan_access_token", "travel_diyuan"),
                java.util.Map.entry("spirit_gathering_array_disk", "deploy_spirit_gather_disk"),
                java.util.Map.entry("auction_invitation", "open_auction_invite"),
                java.util.Map.entry("sect_identity_token", "show_sect_identity"),
                java.util.Map.entry("star_palace_patrol_seal", "star_palace_patrol"),
                java.util.Map.entry("void_palace_map_fragment", "discover_void_palace"),
                java.util.Map.entry("fallen_demon_scout_report", "discover_fallen_demon"),
                java.util.Map.entry("kunwu_map_scroll", "discover_kunwu"),
                java.util.Map.entry("mortal_medicine", "restore_health"));
        cases.forEach((id, effect) -> {
            var definition = com.xunxian.seekingimmortals.registry.BulkItemClassifier
                    .consumable(id);
            assertTrue(definition.isPresent(), id + " must be an executable consumable");
            assertEquals(effect, definition.orElseThrow().effect(), id);
        });
        // Receipt keeps its catalog effect but must be executable.
        var receipt = com.xunxian.seekingimmortals.registry.BulkItemClassifier
                .consumable("star_palace_tax_receipt");
        assertTrue(receipt.isPresent());
        assertFalse(receipt.orElseThrow().effect().isBlank());
    }

    @Test
    void loreItemsMapToRealChronicleEventsInSource() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "item", "CatalogConsumableService.java"));
        assertTrue(source.contains("\"A4_void_palace_built\""));
        assertTrue(source.contains("\"E_ancient_demon_seal_weak\""));
        assertTrue(source.contains("\"A1_kunwu_peak\""));
        assertTrue(source.contains("WorldpackGameplayService.travel"));
        assertTrue(source.contains("FieldKind.SPIRIT_GATHER"));
    }
}
