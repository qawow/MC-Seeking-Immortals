package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmM09ServiceTest {
    @Test
    void catalogLoadsNineteenAuthorRealms() {
        assertEquals(19, SecretRealmCatalogService.size(),
                "expected 19 author secret realms, got " + SecretRealmCatalogService.size());
        assertTrue(SecretRealmCatalogService.find("blood_forbidden").isPresent());
        assertTrue(SecretRealmCatalogService.find("void_palace").isPresent());
        assertTrue(SecretRealmCatalogService.find("seven_meridian_cave").isPresent());
        assertTrue(SecretRealmCatalogService.find("chaotic_sea_abyss_rift").isPresent());
    }

    @Test
    void deepDiveLayersAndTrapsPresent() {
        SecretRealmCatalogService.RealmDef blood = SecretRealmCatalogService.find("blood_forbidden").orElseThrow();
        assertTrue(blood.layersCount() >= 3);
        assertFalse(blood.layers().isEmpty());
        assertFalse(blood.bosses().isEmpty());
        assertEquals("blood_forbidden_gate", blood.gate());
        int traps = blood.layers().stream().mapToInt(layer -> layer.traps().size()).sum();
        assertTrue(traps > 0, "blood_forbidden should expose M07 trap field kinds");
        assertTrue(SecretRealmTrapService.parseKind("ILLUSION_MAZE") != null);
        assertTrue(SecretRealmTrapService.parseKind("SEAL_DEMON") != null);
    }

    @Test
    void sevenGatesBindAtLeastOneRealm() {
        for (String gate : List.of(
                "blood_forbidden_gate",
                "cycle_gate",
                "ancient_rift_gate",
                "nether_ferry_gate",
                "hidden_rift_gate",
                "king_territory_gate",
                "ascension_gate")) {
            assertTrue(SecretRealmCatalogService.primaryRealmForGate(gate).isPresent(),
                    "gate missing binding: " + gate);
        }
    }

    @Test
    void worldpackContainsAuthorNineteenAndRegionRefs() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        Set<String> ids = new HashSet<>();
        for (WorldpackDataService.SecretRealm realm : snapshot.secretRealms()) {
            ids.add(realm.id());
            assertTrue(snapshot.findRegion(realm.regionId()).isPresent(),
                    "missing region for " + realm.id() + " -> " + realm.regionId());
        }
        for (String required : List.of(
                "blood_forbidden", "void_palace", "fallen_demon_valley", "kunwu_mountain",
                "nether_river_land", "guanghan_realm", "demon_gold_mountain", "minor_asura_realm",
                "diyuan", "jiuxian_seclusion", "ancient_cultivator_ruins", "wild_ancient_tomb",
                "thousand_bamboo_puppet_tower", "wild_ancient_ruins", "tianlan_secret_grotto",
                "chaotic_sea_abyss_rift", "spirit_grass_valley", "yin_mountain_catacomb",
                "seven_meridian_cave")) {
            assertTrue(ids.contains(required), "worldpack missing author realm " + required);
        }
        assertTrue(snapshot.secretRealms().size() >= 19);
    }

    @Test
    void bossLootTablesParseAndUniqueRedline() {
        assertTrue(BossLootService.size() >= 12);
        assertTrue(BossLootService.find("blood_jiao_guardian").isPresent());
        assertTrue(BossLootService.find("void_palace_lord").isPresent());

        List<BossLootService.DropDef> first = BossLootService.dropsFor("void_palace_lord", true);
        List<BossLootService.DropDef> repeat = BossLootService.dropsFor("void_palace_lord", false);
        assertFalse(first.isEmpty());
        // unique/first_clear_only must not leak into repeat table
        assertTrue(repeat.stream().noneMatch(drop -> drop.unique() || drop.firstClearOnly()),
                "repeat drops still contain unique flags");
        assertTrue(repeat.stream().noneMatch(drop -> BossLootService.isForbidden(drop.itemId())));
        // void_key style uniques present on first if catalog marks them
        boolean anyUnique = first.stream().anyMatch(drop -> drop.unique() || drop.firstClearOnly()
                || drop.itemId().toLowerCase().contains("void_key"));
        assertTrue(anyUnique || first.size() >= repeat.size());
    }

    @Test
    void dimensionMapCoversMajorCatalogRealms() {
        assertTrue(SecretRealmDimensionService.dimensionIdFor("blood_forbidden").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("void_palace").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("nether_river_land").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("yinming_pocket").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("diyuan").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("minor_asura_realm").isPresent());
        assertTrue(SecretRealmDimensionService.dedicatedDimensionCount() >= 10);
    }

    @Test
    void realmMinNormalizationAcceptsAuthorDesignIds() {
        assertEquals("FOUNDATION", SecretRealmSessionService.normalizeRealmMin("FOUNDATION"));
        assertEquals("QI_REFINING", SecretRealmSessionService.normalizeRealmMin("qi_refining"));
        assertEquals("CORE_FORMATION", SecretRealmSessionService.normalizeRealmMin("CORE_FORMATION"));
    }
}
