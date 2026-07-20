package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogServiceTest {
    @Test
    void loadsAliasesAndBulkCarrierMetadata() {
        ItemCatalogService.Snapshot snapshot = ItemCatalogService.builtin();
        assertTrue(snapshot.aliasCount() >= 5, "expected corpus + block aliases");
        assertTrue(snapshot.carrierCount() >= 1100, "expected expanded bulk carriers, got " + snapshot.carrierCount());
        assertTrue(snapshot.carriers().containsKey("altar_stone"));
        assertTrue(snapshot.carriers().containsKey("dingshen_fu"));
        assertTrue(snapshot.carriers().containsKey("palm_heaven_bottle_stand"));
        assertTrue(snapshot.carriers().containsKey("market_stall_counter"));
    }

    @Test
    void resolvesAliasIdsToCanonicalPaths() {
        assertEquals("yellow_essence", ItemCatalogService.resolveId("yellow_essence_grass"));
        assertEquals("kunwu_copper", ItemCatalogService.resolveId("kunwu_spirit_copper"));
        assertEquals("alchemy_furnace", ItemCatalogService.resolveId("alchemy_furnace_g1"));
        assertEquals("refinement_forge", ItemCatalogService.resolveId("refinement_forge_g1"));
        assertEquals("yin_essence_ore", ItemCatalogService.resolveId("yin_essence_ore_block"));
        assertEquals("sect_earth_fire_room", ItemCatalogService.resolveId("earth_fire_alchemy_room"));
        assertEquals("yellow_essence", ItemCatalogService.resolveId("seeking_immortals:yellow_essence_grass"));
        // 0.2.68 currency / station / component hard aliases
        assertEquals("low_spirit_stone", ItemCatalogService.resolveId("spirit_stone_low"));
        assertEquals("mid_spirit_stone", ItemCatalogService.resolveId("spirit_stone_mid"));
        assertEquals("high_spirit_stone", ItemCatalogService.resolveId("spirit_stone_high"));
        assertEquals("spirit_stone_shard", ItemCatalogService.resolveId("spirit_stone"));
        assertEquals("spirit_stone_shard", ItemCatalogService.resolveId("spirit_shard"));
        assertEquals("immortal_jade", ItemCatalogService.resolveId("jade_immortal"));
        assertEquals("black_iron", ItemCatalogService.resolveId("xuan_iron"));
        assertEquals("ironwood", ItemCatalogService.resolveId("iron_wood"));
        assertEquals("alchemy_furnace", ItemCatalogService.resolveId("alchemy_furnace_g2"));
        assertEquals("refinement_forge", ItemCatalogService.resolveId("refinement_forge_g3"));
    }

    @Test
    void exposesTalismanGradeMetadata() {
        assertTrue(ItemCatalogService.findMeta("anti_demon_talisman").isPresent());
        assertEquals("mid", ItemCatalogService.findMeta("anti_demon_talisman").orElseThrow().grade());
        assertTrue(ItemCatalogService.findMeta("dingshen_fu").map(ItemCatalogService.CarrierMeta::hasGrade).orElse(false));
        assertEquals("huang", ItemCatalogService.findMeta("dingshen_fu").orElseThrow().grade());
    }

    @Test
    void blocksUniqueForbiddenStoryItems() {
        assertTrue(ItemCatalogService.isUniqueForbidden("palm_heaven_bottle"));
        assertTrue(ItemCatalogService.isUniqueForbidden("green_liquid"));
        assertNull(ItemCatalogService.resolveCatalogItem("palm_heaven_bottle"));
        assertFalse(ItemCatalogService.builtin().carriers().containsKey("palm_heaven_bottle"));
        assertFalse(ItemCatalogService.builtin().carriers().containsKey("green_liquid"));
    }

    @Test
    void resolveCatalogItemHandlesBlankAndUnknownSafely() {
        assertNull(ItemCatalogService.resolveId(null));
        assertNull(ItemCatalogService.resolveId(""));
        assertNull(ItemCatalogService.resolveCatalogItem("definitely_not_a_real_item_zzz"));
        // Registry may be unavailable in pure unit tests; method must not throw.
        ItemCatalogService.resolveCatalogItem("spirit_iron");
        assertNotNull(ItemCatalogService.resolveId("spirit_iron"));
        assertTrue(ItemCatalogService.isKnownAlias("yellow_essence_grass"));
    }
}
