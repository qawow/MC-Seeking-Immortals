package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectWarServiceTest {
    @Test
    void warPhaseUsesElapsedFractionWhenStartKnown() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.startedAtGameTime = 1000L;
        data.endsAtGameTime = 1000L + 20L * 60L * 10L; // 10 min window

        long early = data.startedAtGameTime + 20L * 60L; // 1 min in (~10%)
        long mid = data.startedAtGameTime + 20L * 60L * 5L; // 5 min (~50%)
        long late = data.startedAtGameTime + 20L * 60L * 9L; // 9 min (~90%)

        assertEquals(1, SectWarService.warPhase(data, early));
        assertEquals(2, SectWarService.warPhase(data, mid));
        assertEquals(3, SectWarService.warPhase(data, late));
    }

    @Test
    void warPhaseFallsBackToRemainBucketsWithoutStart() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.startedAtGameTime = 0L;
        data.endsAtGameTime = 20L * 60L * 20L;

        assertEquals(1, SectWarService.warPhase(data, 0L)); // 20 min remain
        assertEquals(2, SectWarService.warPhase(data, 20L * 60L * 16L)); // 4 min remain
        assertEquals(3, SectWarService.warPhase(data, 20L * 60L * 19L)); // 1 min remain
    }

    @Test
    void warDataReportsThirdArmyWhenPresent() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.factionC = "";
        assertFalse(data.hasThirdArmy());
        assertEquals(2, data.armyCount());
        data.factionC = "tianlan";
        assertTrue(data.hasThirdArmy());
        assertEquals(3, data.armyCount());
    }

    @Test
    void dailyEventOwnershipRoundTripsAndArmyMatchingIsOrderIndependent() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.factionA = "mulan_council";
        data.factionB = "tianlan_temple";
        data.dailyEventOwned = true;
        data.endsAtGameTime = 24000L;

        assertTrue(SectWarService.sameArmies(data, "tianlan_temple", "mulan_council", ""));
        assertFalse(SectWarService.sameArmies(data, "star_palace", "inverse_star_alliance", ""));

        net.minecraft.nbt.CompoundTag tag = data.save(new net.minecraft.nbt.CompoundTag());
        SectWarService.WarData loaded = SectWarService.WarData.load(tag);
        assertTrue(loaded.dailyEventOwned);
        assertEquals(data.endsAtGameTime, loaded.endsAtGameTime);
    }

    @Test
    void expiryTreatsEndTickAsOutsideTheWar() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.endsAtGameTime = 100L;

        assertFalse(SectWarService.isExpired(data, 99L));
        assertTrue(SectWarService.isExpired(data, 100L));
        assertTrue(SectWarService.isExpired(data, 101L));

        data.active = false;
        assertFalse(SectWarService.isExpired(data, 100L));
    }

    @Test
    void shellGenerationMustMatchCurrentWarAndSurvivesNbt() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.generation = 17L;
        data.scopeRegionId = "mulan_grassland";
        data.scopeDimensionId = "minecraft:overworld";

        net.minecraft.nbt.CompoundTag saved = data.save(new net.minecraft.nbt.CompoundTag());
        SectWarService.WarData loaded = SectWarService.WarData.load(saved);
        assertEquals(17L, loaded.generation);
        assertEquals("mulan_grassland", loaded.scopeRegionId);
        assertEquals("minecraft:overworld", loaded.scopeDimensionId);
        assertEquals(1, loaded.scopes.size());

        net.minecraft.nbt.CompoundTag oldShell = new net.minecraft.nbt.CompoundTag();
        oldShell.putLong(SectWarService.WAR_SHELL_GENERATION, 16L);
        assertFalse(SectWarService.warShellGenerationMatches(oldShell, loaded));
        oldShell.putLong(SectWarService.WAR_SHELL_GENERATION, 17L);
        assertTrue(SectWarService.warShellGenerationMatches(oldShell, loaded));
        assertFalse(SectWarService.warShellGenerationMatches(new net.minecraft.nbt.CompoundTag(), loaded));
    }

    @Test
    void dailyWarScopeRequiresBothRegionAndDimension() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.dailyEventOwned = true;
        data.scopeRegionId = "mulan_grassland";
        data.scopeDimensionId = "minecraft:overworld";

        assertTrue(SectWarService.isInWarScope(data, "mulan_grassland", "minecraft:overworld"));
        assertFalse(SectWarService.isInWarScope(data, "tiannan", "minecraft:overworld"));
        assertFalse(SectWarService.isInWarScope(data, "mulan_grassland", "seeking_immortals:chaotic_sea"));

        data.dailyEventOwned = false;
        assertTrue(SectWarService.isInWarScope(data, "tiannan", "seeking_immortals:chaotic_sea"));
    }

    @Test
    void legacyScopeMigratesAndMultipleScopesRoundTrip() {
        net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
        legacy.putBoolean("Active", true);
        legacy.putBoolean("DailyEventOwned", true);
        legacy.putLong("Generation", 3L);
        legacy.putString("ScopeRegion", "mulan_grassland");
        legacy.putString("ScopeDimension", "minecraft:overworld");

        SectWarService.WarData data = SectWarService.WarData.load(legacy);
        assertEquals(1, data.scopes.size());
        assertTrue(SectWarService.addDailyScope(
                data, "chaotic_sea", "seeking_immortals:chaotic_sea"));

        net.minecraft.nbt.CompoundTag saved = data.save(new net.minecraft.nbt.CompoundTag());
        assertEquals("mulan_grassland", saved.getString("ScopeRegion"));
        assertEquals("minecraft:overworld", saved.getString("ScopeDimension"));
        assertEquals(2, saved.getList("Scopes", net.minecraft.nbt.Tag.TAG_COMPOUND).size());

        SectWarService.WarData loaded = SectWarService.WarData.load(saved);
        assertEquals(2, loaded.scopes.size());
        assertTrue(SectWarService.isInWarScope(loaded, "mulan_grassland", "minecraft:overworld"));
        assertTrue(SectWarService.isInWarScope(
                loaded, "chaotic_sea", "seeking_immortals:chaotic_sea"));
    }

    @Test
    void sameDailyWarMergesScopesWithoutResettingSharedState() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.dailyEventOwned = true;
        data.factionA = "star_palace";
        data.factionB = "inverse_star_alliance";
        data.scoreA = 9;
        data.scoreB = 6;
        data.generation = 12L;
        data.startedAtGameTime = 20L;
        data.endsAtGameTime = 100L;
        data.scopeRegionId = "chaotic_sea";
        data.scopeDimensionId = "minecraft:overworld";

        assertTrue(SectWarService.mergeDailyWarScope(
                data, "inverse_star_alliance", "star_palace",
                "outer_sea_market", "seeking_immortals:chaotic_sea", 180L, 50L));

        assertEquals(2, data.scopes.size());
        assertEquals(9, data.scoreA);
        assertEquals(6, data.scoreB);
        assertEquals(12L, data.generation);
        assertEquals(20L, data.startedAtGameTime);
        assertEquals(180L, data.endsAtGameTime);
        assertTrue(SectWarService.isInWarScope(data, "chaotic_sea", "minecraft:overworld"));
        assertTrue(SectWarService.isInWarScope(
                data, "outer_sea_market", "seeking_immortals:chaotic_sea"));

        assertTrue(SectWarService.mergeDailyWarScope(
                data, "star_palace", "inverse_star_alliance",
                "outer_sea_market", "seeking_immortals:chaotic_sea", 160L, 60L));
        assertEquals(2, data.scopes.size());
        assertEquals(180L, data.endsAtGameTime);
    }

    @Test
    void differentArmiesAndManualWarsCannotMergeDailyScopes() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.dailyEventOwned = true;
        data.factionA = "star_palace";
        data.factionB = "inverse_star_alliance";
        data.endsAtGameTime = 200L;
        assertTrue(SectWarService.addDailyScope(data, "chaotic_sea", "minecraft:overworld"));

        assertFalse(SectWarService.mergeDailyWarScope(
                data, "mulan_council", "tianlan_temple",
                "mulan_grassland", "minecraft:overworld", 300L, 50L));
        assertEquals(1, data.scopes.size());
        assertEquals(200L, data.endsAtGameTime);

        data.dailyEventOwned = false;
        assertFalse(SectWarService.mergeDailyWarScope(
                data, "star_palace", "inverse_star_alliance",
                "outer_sea_market", "minecraft:overworld", 300L, 50L));
        assertEquals(1, data.scopes.size());
        assertEquals(200L, data.endsAtGameTime);
    }

    @Test
    void legacyUnscopedDailyWarCanRenewButPartialScopeIsRejected() {
        SectWarService.WarData data = new SectWarService.WarData();
        data.active = true;
        data.dailyEventOwned = true;
        data.factionA = "star_palace";
        data.factionB = "inverse_star_alliance";
        data.endsAtGameTime = 100L;

        assertTrue(SectWarService.mergeDailyWarScope(
                data, "star_palace", "inverse_star_alliance", "", "", 160L, 50L));
        assertEquals(160L, data.endsAtGameTime);
        assertTrue(data.scopes.isEmpty());

        assertFalse(SectWarService.mergeDailyWarScope(
                data, "star_palace", "inverse_star_alliance",
                "chaotic_sea", "", 200L, 60L));
        assertEquals(160L, data.endsAtGameTime);
        assertTrue(data.scopes.isEmpty());
    }

    @Test
    void dailyWarScopeSetIsBoundedAndDeduplicated() {
        SectWarService.WarData data = new SectWarService.WarData();
        for (int i = 0; i < SectWarService.MAX_DAILY_WAR_SCOPES; i++) {
            assertTrue(SectWarService.addDailyScope(
                    data, "region_" + i, "seeking_immortals:scope_" + i));
        }
        assertEquals(SectWarService.MAX_DAILY_WAR_SCOPES, data.scopes.size());
        assertTrue(SectWarService.addDailyScope(
                data, "region_0", "seeking_immortals:scope_0"));
        assertFalse(SectWarService.addDailyScope(
                data, "overflow", "seeking_immortals:overflow"));
        assertEquals(SectWarService.MAX_DAILY_WAR_SCOPES, data.scopes.size());
    }

    @Test
    void generationAdvancesAndWrapsToAPositiveToken() {
        assertEquals(1L, SectWarService.nextGeneration(0L));
        assertEquals(42L, SectWarService.nextGeneration(41L));
        assertEquals(1L, SectWarService.nextGeneration(Long.MAX_VALUE));
    }
}
