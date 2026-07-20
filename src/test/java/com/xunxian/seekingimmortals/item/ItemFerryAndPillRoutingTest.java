package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 item routing fixes: ferry pass consumable, ferry fee tokens not vehicles,
 * alias pills classify as PILL.
 */
class ItemFerryAndPillRoutingTest {
    @Test
    void ferryPassIsExecutableConsumableAndRegisteredAsCatalogConsumable() throws Exception {
        assertTrue(BulkItemClassifier.consumable("ferry_pass").isPresent());
        assertEquals("travel_nether_ferry",
                BulkItemClassifier.consumable("ferry_pass").orElseThrow().effect());
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "registry", "ModItems.java"));
        assertTrue(source.contains("FERRY_PASS"));
        assertTrue(source.contains("CatalogConsumableItem"));
        assertTrue(source.contains("ferry_pass"));
        assertFalse(source.contains("FERRY_PASS = ITEMS.register(\"ferry_pass\", () -> new Item("));
    }

    @Test
    void netherFerryFeeTokensAreNotBoardableEquipment() {
        assertFalse(BulkItemClassifier.isEquipment("nether_ferry_coin", "material"));
        assertFalse(BulkItemClassifier.isEquipment("nether_ferry_token", "material"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("nether_ferry_coin", "material"));
        assertEquals(BulkItemKind.CARRIER, BulkItemClassifier.classify("nether_ferry_token", "material"));
        assertEquals(CatalogEquipmentService.Mode.UNKNOWN,
                CatalogEquipmentService.resolveMode("nether_ferry_coin"));
        assertEquals(CatalogEquipmentService.Mode.UNKNOWN,
                CatalogEquipmentService.resolveMode("nether_ferry_token"));
        // Real vehicles still classify.
        assertTrue(BulkItemClassifier.isEquipment("spirit_boat_low", "equipment")
                || BulkItemClassifier.isEquipment("spirit_boat", "equipment")
                || BulkItemClassifier.classify("cloud_sedan", "equipment") == BulkItemKind.EQUIPMENT);
        assertEquals(CatalogEquipmentService.Mode.VEHICLE,
                CatalogEquipmentService.resolveMode("spirit_boat_low"));
    }

    @Test
    void aliasPillsResolveThroughPillEffectCatalog() {
        assertTrue(PillEffectCatalog.findByPillId("appearance_lock_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("marrow_drain_pill").isPresent());
        assertTrue(PillEffectCatalog.findByPillId("qingxu_pill").isPresent());
        assertEquals("freeze_appearance",
                PillEffectCatalog.findByPillId("appearance_lock_pill").orElseThrow().effect());
        assertEquals("power_now_lifespan_debt",
                PillEffectCatalog.findByPillId("marrow_drain_pill").orElseThrow().effect());
        assertEquals("pet_mind_clarity",
                PillEffectCatalog.findByPillId("qingxu_pill").orElseThrow().effect());
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("appearance_lock_pill", "pill"));
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("marrow_drain_pill", "pill"));
        assertEquals(BulkItemKind.PILL, BulkItemClassifier.classify("qingxu_pill", "pill"));
    }
}
