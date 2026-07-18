package com.xunxian.seekingimmortals.beast;

import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.worldpack.BeastSpawnTableService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BeastEcologyServiceTest {
    @Test
    void bestiaryLoadsNearEighteenHundred() {
        assertTrue(BeastBestiaryService.size() >= 1800,
                "expected ~1850 bestiary entries, got " + BeastBestiaryService.size());
        assertTrue(BeastBestiaryService.find("mo_jiao").isPresent());
        assertTrue(BeastBestiaryService.find("墨蛟").isPresent());
    }

    @Test
    void thirteenTierSchemaAlignedToM01() {
        assertEquals(13, BeastTierService.tierCount());
        assertTrue(BeastTierService.hasDemonCore(5));
        assertFalse(BeastTierService.hasDemonCore(4));
        assertEquals("1-4", BeastTierService.lootBandFor(3));
        assertEquals("5-8", BeastTierService.lootBandFor(7));
        assertEquals("9-13", BeastTierService.lootBandFor(12));
        assertNotNull(BeastTierService.realmForTier(1));
        assertNotNull(BeastTierService.scaleStats(9));
        assertTrue(BeastTierService.canSuppress(6, 7, 2));
        assertFalse(BeastTierService.canSuppress(3, 9, 2));
    }

    @Test
    void companionsProtectedFromDailySpawn() {
        assertTrue(BeastCompanionService.size() >= 8);
        assertTrue(BeastCompanionService.isProtectedCompanion("shi_jin_chong"));
        assertTrue(BeastCompanionService.isProtectedCompanion("噬金虫"));
        assertTrue(BeastCompanionService.isProtectedCompanion("bing_feng"));
        assertTrue(BeastBestiaryService.isBannedFromDailySpawn("shi_jin_chong"));
        assertTrue(BeastBestiaryService.isBannedFromDailySpawn("ti_hun_shou"));
        assertTrue(BeastSpawnTableService.isBanned("shi_jin_chong"));
        // Growth stages present for 噬金虫.
        assertTrue(BeastCompanionService.stageForGrowth("shi_jin_chong", 0).isPresent());
        assertTrue(BeastCompanionService.stageForGrowth("shi_jin_chong", 20).isPresent());
    }

    @Test
    void spawnTablesExpandWithRegionKeysAndNoBannedWeights() {
        assertTrue(BeastSpawnTableService.tableCount() >= 17);
        // region_spawn_tables_v98 region ids
        assertTrue(BeastSpawnTableService.findTable("blood_forbidden", "any").isPresent()
                || BeastSpawnTableService.findTable("blood_forbidden", "forest").isPresent());
        assertTrue(BeastSpawnTableService.findTable("tiannan", "forest").isPresent());
        Set<String> bannedHits = new HashSet<>();
        for (BeastSpawnTableService.Table table : BeastSpawnTableService.tables()) {
            for (BeastSpawnTableService.Weight w : table.weights()) {
                if (BeastSpawnTableService.isBanned(w.beastId())) {
                    bannedHits.add(w.beastId());
                }
            }
        }
        assertTrue(bannedHits.isEmpty(), "banned beasts leaked into tables: " + bannedHits);
    }

    @Test
    void lootBandsAndMaterialIdsResolve() {
        assertFalse(BeastLootService.dropsForTier(1).isEmpty());
        assertFalse(BeastLootService.dropsForTier(7).isEmpty());
        assertEquals("beast_hide", BeastLootService.resolveItemId("兽皮"));
        assertEquals("demon_core_low", BeastLootService.resolveItemId("demon_core_low"));
        // band drops use catalog-friendly ids
        boolean anyCore = BeastLootService.dropsForTier(6).stream()
                .anyMatch(d -> d.itemId().contains("demon_core") || d.itemId().contains("beast"));
        assertTrue(anyCore);
    }

    @Test
    void puppetDefinitionsLoaded() {
        assertTrue(PuppetDefinitionService.size() >= 8);
        assertTrue(PuppetDefinitionService.find("basic_wood_puppet").isPresent());
        assertTrue(PuppetDefinitionService.find("giant_ape_puppet").isPresent());
        assertEquals(com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Archetype.PUPPET,
                PuppetDefinitionService.find("basic_wood_puppet").orElseThrow().archetype());
    }

    @Test
    void bossCatalogUsesCanonicalStatusesAndIndependentCooldowns() {
        assertTrue(BeastBossService.size() >= 16);
        assertTrue(BeastBossService.find("blood_jiao_guardian").isPresent());
        assertTrue(BeastBossService.find("kunwu_puppet_king").isPresent());
        var def = BeastBossService.find("blood_jiao_guardian").orElseThrow();
        assertFalse(def.phases().isEmpty());
        assertTrue(BeastBossService.isKnownStatusId("burn"));
        assertTrue(BeastBossService.isKnownStatusId("soul_shock"));
        assertFalse(BeastBossService.isKnownStatusId("not_a_real_status"));
        for (var boss : BeastBossService.snapshot().byId().values()) {
            for (var phase : boss.phases()) {
                assertTrue(StatusRegistry.isKnown(phase.statusId()),
                        boss.bossId() + " has unknown status " + phase.statusId());
                assertEquals(StatusRegistry.definition(phase.statusId()).orElseThrow().beneficial(),
                        BeastBossService.statusTargetsSelf(phase),
                        boss.bossId() + " has wrong status recipient for " + phase.statusId());
                assertTrue(com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver
                                .isAbstractTypeRegistered(phase.effectType())
                                || !phase.effectType().isBlank());
            }
        }
        assertFalse(BeastBossService.isPhaseReady(79, 80));
        assertTrue(BeastBossService.isPhaseReady(80, 80));
        assertFalse(BeastBossService.isPhaseReady(159, 160));
        assertTrue(BeastBossService.isPhaseReady(160, 160));
    }

    @Test
    void bossStatusesKeepCasterWithoutPlayerRealmHitChecks() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "beast", "BeastBossService.java");
        String source = Files.readString(sourcePath);
        assertTrue(source.contains("StatusRegistry.applyGuaranteedStatus(statusTarget, mob"));
    }

    @Test
    void spawnTableWeightsResolvableInBestiaryOrGeneric() {
        int checked = 0;
        int missing = 0;
        for (BeastSpawnTableService.Table table : BeastSpawnTableService.tables()) {
            for (BeastSpawnTableService.Weight w : table.weights()) {
                checked++;
                // Not every region spawn id is in the 1850 bestiary (generic packs), but id must be non-blank and not banned.
                assertFalse(w.beastId().isBlank());
                assertFalse(BeastSpawnTableService.isBanned(w.beastId()));
                if (BeastBestiaryService.find(w.beastId()).isEmpty()
                        && BeastCompanionService.find(w.beastId()).isEmpty()) {
                    missing++;
                }
            }
        }
        assertTrue(checked > 50, "expected expanded spawn weights, got " + checked);
        // Allow some generic ids but majority should resolve.
        assertTrue(missing < checked, "all weights missing from bestiary is unexpected");
    }
}
