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
}
