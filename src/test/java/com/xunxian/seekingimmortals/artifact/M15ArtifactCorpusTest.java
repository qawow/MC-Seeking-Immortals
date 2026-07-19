package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.FlyingArtifactItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M15 acceptance: 217 artifacts load with tier/synergy/drops/auction/draft;
 * active skill ids resolve; realm power scale; draft reconcile clean.
 */
class M15ArtifactCorpusTest {

    @Test
    void loadsFullArtifactCorpus() {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        assertEquals(18, ArtifactDataService.sourceFiles().size());
        assertEquals(217, snap.artifacts().size());
        assertEquals(217, snap.sourceFileEntryCounts().get("artifacts_catalog.json"));
        assertEquals(11, snap.elevenTiers().size());
        assertTrue(snap.elevenIdMap().size() >= 160);
        assertFalse(snap.taxonomy().isEmpty());
        assertEquals(11, snap.gradeBands().size());
        assertTrue(snap.synergies().size() >= 15);
        assertTrue(snap.artifactCombos().size() >= 4);
        assertFalse(snap.realmDrops().isEmpty());
        assertFalse(snap.factionSpecialties().isEmpty());
        assertFalse(snap.wanbaoStock().isEmpty());
        assertFalse(snap.auctionLots().isEmpty());
        assertEquals(22, snap.draftItems().size());
        assertFalse(snap.ancientEntries().isEmpty());
        assertEquals(0.25D, snap.realmPowerScale().belowRealmMin());
        assertEquals(0.7D, snap.realmPowerScale().atRealmMin());
        assertEquals(1.0D, snap.realmPowerScale().twoMajorAbove());
    }

    @Test
    void everyCatalogArtifactResolvesTierAndRealm() {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        int unresolved = 0;
        for (ArtifactDataService.ArtifactDefinition def : snap.artifacts().values()) {
            assertNotNull(def.id());
            assertTrue(def.gameTier() >= 1, "game_tier for " + def.id());
            Realm required = ArtifactPowerService.resolveRequiredRealm(def);
            assertNotNull(required, "realm for " + def.id());
            int resolved = snap.resolvedGameTier(def.id());
            if (resolved <= 0) {
                unresolved++;
            }
        }
        assertEquals(0, unresolved);
    }

    @Test
    void activeSkillsMapToTechniqueIds() {
        // Pure mapping is unit-test safe (no SkillEffectRegistry / SoundEvents init).
        assertTrue(ArtifactActiveSkillService.hasTechniqueMapping("flying_sword_low"));
        assertTrue(ArtifactActiveSkillService.hasTechniqueMapping("wind_escape_sail"));
        assertTrue(ArtifactActiveSkillService.hasTechniqueMapping("silver_giant_sword"));
        assertTrue(ArtifactActiveSkillService.hasTechniqueMapping("gold_demon_chain"));
        assertEquals("flying_sword_strike",
                ArtifactActiveSkillService.mapTechniqueId(
                        ArtifactDataService.builtin().findArtifact("flying_sword_low").orElseThrow()));
        assertTrue(ArtifactActiveSkillService.mappedTechniqueCount() >= 80,
                "expected broad active skill mapping, got " + ArtifactActiveSkillService.mappedTechniqueCount());
    }

    @Test
    void synergyCombosApplyAttackDefenseBonuses() {
        Set<String> held = new HashSet<>();
        held.add("yellow_umbrella");
        held.add("black_gold_shield");
        ArtifactSynergyService.SynergyBonus bonus = ArtifactSynergyService.evaluateIds(held);
        assertTrue(bonus.defenseMultiplier() > 1.0D, "defense stack combo");
        assertTrue(bonus.activeRelations().stream().anyMatch(r -> r.contains("defense_stack")));

        held.clear();
        held.add("gold_demon_chain");
        held.add("peerless_flying_knives");
        bonus = ArtifactSynergyService.evaluateIds(held);
        assertTrue(bonus.attackMultiplier() > 1.0D, "multi projectile synergy");
    }

    @Test
    void realmPowerScaleSuppressesBelowMin() {
        ArtifactDataService.ArtifactDefinition high = ArtifactDataService.builtin()
                .findArtifact("xuanguang_mirror").orElseThrow();
        // Simulate scales without player: resolveRequiredRealm should be high.
        Realm required = ArtifactPowerService.resolveRequiredRealm(high);
        assertTrue(required.ordinal() >= Realm.CORE_FORMATION.ordinal()
                || high.gameTier() >= 7);

        assertEquals(0.25D, ArtifactDataService.builtin().realmPowerScale().belowRealmMin());
        assertTrue(ArtifactPowerService.scaledSpiritualCost(10, 0.25D) > 10);
        assertTrue(ArtifactPowerService.scaledDamage(20.0D, 0.25D) < 20.0D);
        assertTrue(ArtifactPowerService.scaledCooldown(100, 0.25D) > 100);
    }

    @Test
    void draftReconcileIsCleanForKnownRegistry() {
        ArtifactDraftReconcileService.DiffReport report = ArtifactDraftReconcileService.reconcile();
        assertEquals(22, report.draftCount());
        assertEquals(217, report.catalogCount());
        assertEquals(0, report.draftMissing(),
                "draft missing from registry: " + report.missingFromRegistry());
        assertTrue(report.isClean());
        assertTrue(report.uniqueRestricted().size() >= 10,
                "spirit/ancient treasures should be unique-restricted");
    }

    @Test
    void flyingCapabilityCoversKnownFlyers() {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        assertTrue(snap.isFlyingCapable(FlyingArtifactItem.FLYING_SWORD_ARTIFACT_ID));
        assertTrue(snap.isFlyingCapable(FlyingArtifactItem.FLYING_ARTIFACT_ID));
        assertTrue(snap.isFlyingCapable("cloud_boots"));
        assertTrue(snap.isFlyingCapable("silver_giant_sword")
                || "flying_sword".equals(snap.findArtifact("silver_giant_sword").map(a -> a.type()).orElse("")));
    }

    @Test
    void ownershipTagsAndSpiritThresholdsAreDefined() {
        assertEquals(5, ArtifactOwnershipService.SPIRIT_AWAKEN_LAYER);
        assertEquals(Realm.CORE_FORMATION, ArtifactOwnershipService.SPIRIT_AWAKEN_REALM);
        assertEquals(9, ArtifactOwnershipService.MAX_REFINEMENT_LAYER);
        assertEquals("SeekingImmortalsArtifactOwner", ArtifactOwnershipService.OWNER_UUID_TAG);
    }

    @Test
    void synergyRejectsUnequippableOrBrokenArtifacts() {
        assertTrue(ArtifactSynergyService.isEligibleForSynergy(
                false, true, false, false, 100, 2, 2));
        assertFalse(ArtifactSynergyService.isEligibleForSynergy(
                false, false, true, false, 100, 2, 2));
        assertFalse(ArtifactSynergyService.isEligibleForSynergy(
                false, true, false, true, 100, 8, 8));
        assertFalse(ArtifactSynergyService.isEligibleForSynergy(
                false, true, true, true, 0, 8, 8));
        assertFalse(ArtifactSynergyService.isEligibleForSynergy(
                false, true, true, true, 100, 1, 2));
        assertFalse(ArtifactSynergyService.isEligibleForSynergy(
                false, true, false, false, 100, -1, 0));
        assertTrue(ArtifactSynergyService.isEligibleForSynergy(
                true, false, false, true, 0, 0, 10));
    }

    @Test
    void runtimeSynergyScansOnlyEquippedUsableArtifacts() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactSynergyService.java");
        String source = Files.readString(sourcePath);
        assertTrue(source.contains("player.getMainHandItem()"));
        assertTrue(source.contains("player.getOffhandItem()"));
        assertTrue(source.contains("CuriosApi.getCuriosInventory(player)"));
        assertFalse(source.contains("player.getInventory().items"));

        String activation = Files.readString(sourcePath.resolveSibling("ArtifactActivationService.java"));
        assertTrue(activation.contains("hasUsableIntegrity(integrity, info.integrityCost(), false)"));
    }

    @Test
    void auctionAndDropPoolsReferenceCatalogIds() {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        int badStock = 0;
        for (ArtifactDataService.AuctionStock stock : snap.wanbaoStock()) {
            if (snap.findArtifact(stock.artifactId()).isEmpty()) {
                badStock++;
            }
        }
        int badLots = 0;
        for (ArtifactDataService.AuctionLot lot : snap.auctionLots()) {
            if (snap.findArtifact(lot.artifactId()).isEmpty()) {
                badLots++;
            }
        }
        int badDrops = 0;
        for (List<ArtifactDataService.DropEntry> pool : snap.realmDrops().values()) {
            for (ArtifactDataService.DropEntry drop : pool) {
                // materials may appear in drop pools; only count pure misses without bulk path
                if (snap.findArtifact(drop.id()).isEmpty() && drop.id() != null && drop.id().contains("artifact")) {
                    badDrops++;
                }
            }
        }
        assertEquals(0, badStock, "wanbao stock must resolve catalog ids");
        assertEquals(0, badLots, "auction lots must resolve catalog ids");
        assertEquals(0, badDrops);
    }

    @Test
    void scaledDamageMultipliesBaseByScale() {
        assertEquals(5.0D, ArtifactPowerService.scaledDamage(20.0D, 0.25D), 1.0e-6D);
        assertEquals(20.0D, ArtifactPowerService.scaledDamage(20.0D, 1.0D), 1.0e-6D);
        assertEquals(0.0D, ArtifactPowerService.scaledDamage(20.0D, 0.0D), 1.0e-6D);
        assertEquals(0.0D, ArtifactPowerService.scaledDamage(20.0D, -1.0D), 1.0e-6D);
    }

    @Test
    void genericActivationDamageSitesUsePowerScale() throws Exception {
        String activation = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactActivationService.java"));
        assertTrue(activation.contains("ArtifactPowerService.scaledDamage"));
        assertTrue(activation.contains("applyActivation(player, cultivation, artifact, info, powerScale)"));
        // every damage assignment must be scaled
        long unscaled = activation.lines()
                .filter(line -> line.contains("double damage =") && !line.contains("scaledDamage"))
                .count();
        assertEquals(0L, unscaled);
    }
}
