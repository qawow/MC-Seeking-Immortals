package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.network.SetTechniqueSlotPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import com.xunxian.seekingimmortals.combat.CombatStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1CultivationSystemTest {
    @Test
    void mapsDesignStageIdsToExistingRealmStageConstants() {
        RealmStage[] qiStages = PlayerCultivation.getStagesForRealmPublic(Realm.QI_REFINING);
        assertEquals(13, qiStages.length);
        for (int index = 0; index < qiStages.length; index++) {
            assertTrue(qiStages[index].isQiRefiningLayer());
            assertEquals("QI_" + (index + 1), qiStages[index].getDesignId());
        }

        RealmStage[] foundationStages = PlayerCultivation.getStagesForRealmPublic(Realm.FOUNDATION_ESTABLISHMENT);
        assertArrayEquals(new RealmStage[] {
                RealmStage.EARLY,
                RealmStage.MIDDLE,
                RealmStage.LATE,
                RealmStage.PEAK
        }, foundationStages);
        assertEquals("FOUNDATION_EARLY", RealmStage.EARLY.getDesignId());
        assertEquals("FOUNDATION_MID", RealmStage.MIDDLE.getDesignId());
        assertEquals("FOUNDATION_LATE", RealmStage.LATE.getDesignId());
        assertEquals("FOUNDATION_PEAK", RealmStage.PEAK.getDesignId());
    }

    @Test
    void validatesPhase1RealmStageBaselineTable() {
        assertPhase1Baseline(Realm.QI_REFINING, 1300, 180, 90, 330);
        assertPhase1Baseline(Realm.FOUNDATION_ESTABLISHMENT, 4200, 540, 230, 780);
        assertPhase1Baseline(Realm.CORE_FORMATION, 18000, 2400, 950, 2200);
    }

    @Test
    void exposesStableCoreAttributeReadWriteCompatibility() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 500));
        long targetCultivation = cultivation.getCurrentStageStartExp() + 20L;
        cultivation.setMana(75);
        cultivation.setDivSense(12);
        cultivation.setBodyRef(4);
        cultivation.setQiDevRisk(0.7F);
        cultivation.setTribRes(0.9F);
        cultivation.setCultivation(targetCultivation);
        cultivation.startTribulation(Realm.CORE_FORMATION, 3, 40);
        cultivation.setGoldCore(GoldCoreGrade.HIGH, 82);

        assertEquals(75, cultivation.getMana());
        assertEquals(12, cultivation.getDivSense());
        assertEquals(4, cultivation.getBodyRef());
        assertEquals(0.7F, cultivation.getQiDevRiskFloat(), 0.0001F);
        assertEquals(70.0F, cultivation.getQiDevRiskPercent(), 0.0001F);
        assertEquals(0.9F, cultivation.getTribResFloat(), 0.0001F);
        assertEquals(90.0F, cultivation.getTribResPercent(), 0.0001F);
        assertEquals(targetCultivation, cultivation.getCultivationLong());
        assertEquals(cultivation.getCurrentStageCapExp(), cultivation.getCultivationMax());
        assertEquals(cultivation.getMaxSpiritualPower(), cultivation.getManaMax());
        assertTrue(cultivation.isTribulationActive());
        assertEquals(Realm.CORE_FORMATION, cultivation.getTribulationTargetRealm());
        assertEquals(3, cultivation.getTribulationTotalStrikes());
        assertEquals(40, cultivation.getTribulationNextStrikeTicks());
        assertEquals(GoldCoreGrade.HIGH, cultivation.getGoldCoreGrade());
        assertEquals(82, cultivation.getGoldCoreScore());

        CompoundTag saved = cultivation.saveNBTData();
        PlayerCultivation loaded = new PlayerCultivation();
        loaded.loadNBTData(saved);

        assertEquals(cultivation.getMana(), loaded.getMana());
        assertEquals(cultivation.getDivSense(), loaded.getDivSense());
        assertEquals(cultivation.getBodyRef(), loaded.getBodyRef());
        assertEquals(cultivation.getQiDevRiskFloat(), loaded.getQiDevRiskFloat(), 0.0001F);
        assertEquals(cultivation.getTribResFloat(), loaded.getTribResFloat(), 0.0001F);
        assertEquals(cultivation.getCultivationLong(), loaded.getCultivationLong());
        assertTrue(loaded.isTribulationActive());
        assertEquals(Realm.CORE_FORMATION, loaded.getTribulationTargetRealm());
        assertEquals(3, loaded.getTribulationTotalStrikes());
        assertEquals(40, loaded.getTribulationNextStrikeTicks());
        assertEquals(GoldCoreGrade.HIGH, loaded.getGoldCoreGrade());
        assertEquals(82, loaded.getGoldCoreScore());
    }

    @Test
    void migratesLegacyNbtCoreAttributeFields() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("SpiritualPower", 42);
        legacy.putInt("DivineConsciousness", 11);
        legacy.putInt("bodyRef", 3);
        legacy.putInt("qiDevRisk", 70);
        legacy.putInt("tribRes", 80);
        legacy.putInt("CultivationExp", 525);
        legacy.putString("Realm", Realm.QI_REFINING.name());
        legacy.putString("Stage", RealmStage.LAYER_1.name());

        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(legacy);

        assertEquals(42, cultivation.getMana());
        assertEquals(11, cultivation.getDivSense());
        assertEquals(3, cultivation.getBodyRef());
        assertEquals(0.7F, cultivation.getQiDevRiskFloat(), 0.0001F);
        assertEquals(0.8F, cultivation.getTribResFloat(), 0.0001F);
        assertEquals(525L, cultivation.getCultivationLong());
    }

    @Test
    void clampsLegacyTribulationResistanceFields() {
        CompoundTag legacy = realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 0);
        legacy.putInt("tribRes", 999);

        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(legacy);

        assertEquals(0.9F, cultivation.getTribResFloat(), 0.0001F);
        assertEquals(90.0F, cultivation.getTribResPercent(), 0.0001F);
    }

    @Test
    void validatesDerivedAttributeCalculations() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.setSpiritualRoot(SpiritualRoot.HEAVENLY);
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 0));

        assertEquals(90, cultivation.getMaxHealthPoints());
        double baseDefense = cultivation.getDefensePower();
        assertEquals(12.0F, cultivation.getManaRecoveryPerSecond(), 0.0001F);
        assertEquals(9.0F, cultivation.getCultivationGainPerSecond(), 0.0001F);
        assertEquals(14.0F, cultivation.getFlyingSpeed(), 0.0001F);
        assertEquals(1.0D, cultivation.getMovementSpeedScale(), 0.0001D);
        assertEquals(cultivation.getMovementSpeedBonus(), cultivation.getEffectiveMovementSpeedBonus(), 0.0001D);
        assertTrue(cultivation.getMeleeAttackPower() > 0.0D);
        assertTrue(cultivation.getDefensePower() > 0.0D);

        cultivation.setBodyRef(100);
        assertTrue(cultivation.getMaxHealthPoints() > 90);
        assertTrue(cultivation.getDefensePower() > baseDefense);
    }

    @Test
    void worldpackDailyCultivationEventsBoostMeditationGain() {
        MeditationFormula.Breakdown tenPerTick = new MeditationFormula.Breakdown(
                0.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 200.0D);

        assertEquals(1.0D, PlayerCultivation.getWorldpackDailyCultivationMultiplier(""), 0.0001D);
        assertEquals(1.10D, PlayerCultivation.getWorldpackDailyCultivationMultiplier("spirit_rain"), 0.0001D);
        assertEquals(1.0D, PlayerCultivation.getWorldpackDailyCultivationMultiplier("starter_spirit_rain"), 0.0001D);
        assertEquals(1.20D, PlayerCultivation.getWorldpackDailyCultivationMultiplier("spirit_vein_pulse"), 0.0001D);

        assertEquals(10, addMeditationGainForWorldpackEvent("", tenPerTick));
        assertEquals(11, addMeditationGainForWorldpackEvent("spirit_rain", tenPerTick));
        assertEquals(10, addMeditationGainForWorldpackEvent("starter_spirit_rain", tenPerTick));
        assertEquals(12, addMeditationGainForWorldpackEvent("spirit_vein_pulse", tenPerTick));
    }

    @Test
    void validatesGoldCoreScoreThresholdsAndAttributeMultiplier() {
        assertEquals(GoldCoreGrade.PSEUDO, GoldCoreGrade.fromScore(34));
        assertEquals(GoldCoreGrade.LOW, GoldCoreGrade.fromScore(35));
        assertEquals(GoldCoreGrade.MIDDLE, GoldCoreGrade.fromScore(55));
        assertEquals(GoldCoreGrade.HIGH, GoldCoreGrade.fromScore(75));
        assertEquals(GoldCoreGrade.PERFECT, GoldCoreGrade.fromScore(90));

        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.CORE_FORMATION, RealmStage.EARLY, 0));
        cultivation.setGoldCore(GoldCoreGrade.PERFECT, 96);
        int boostedHealth = cultivation.getMaxHealthPoints();
        double boostedAttack = cultivation.getMeleeAttackPower();
        CombatStats stats = new CombatStats(cultivation);

        cultivation.setGoldCore(GoldCoreGrade.NONE, 0);
        assertTrue(boostedHealth > cultivation.getMaxHealthPoints());
        assertTrue(boostedAttack > cultivation.getMeleeAttackPower());
        assertEquals(boostedAttack, stats.getBaseAttack(), 0.0001D);
    }

    @Test
    void validatesCompleteFiveElementsAndAdvancedBreakthroughBonus() {
        CompoundTag tag = realmTag(Realm.SOUL_TRANSFORMATION, RealmStage.LATE, 0);
        tag.putString("SpiritualRoot", SpiritualRoot.MIXED.name());
        tag.putString("SpiritualRootAttributes", "METAL,WOOD,WATER,FIRE,EARTH");
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(tag);

        assertTrue(cultivation.hasCompleteFiveElements());
        assertTrue(cultivation.getAdvancedBreakthroughBonus(Realm.VOID_REFINEMENT) >= 0.08D);
    }

    @Test
    void syncCultivationPacketRoundTripsHighRealmFields() {
        SyncCultivationDataPacket packet = new SyncCultivationDataPacket(
                "结丹", "初期", 100, 200, 10, 10, 1000L, 100, 200, 30, 12,
                5, 8, 500, 30, 470, "五灵根", "金/木/水/火/土", 80,
                true, true, "五雷之体", "上品金丹", 82, true, true,
                "结丹", 1, 3, 40, 4, false, false, 0, false, 0,
                1.5D, 1.2D, 1.1D, 33.0D, 22.0D, 0.2D, 1.6D, 0.1D,
                0.95D, 1.0D, 0.05D, 150, "天地灵气", 0.65D, 0.2D,
                0.15D, 0.08D, 0.05D, 2, 1.0D, 1.1D, 1.2D, 1.3D,
                1.4D, 1.5D, 6.0D, 7.0D);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncCultivationDataPacket.encode(packet, buffer);
        SyncCultivationDataPacket decoded = SyncCultivationDataPacket.decode(buffer);

        assertEquals("上品金丹", decoded.goldCoreGrade());
        assertEquals(82, decoded.goldCoreScore());
        assertTrue(decoded.completeFiveElements());
        assertTrue(decoded.tribulationActive());
        assertEquals("结丹", decoded.tribulationTargetRealm());
        assertEquals(1, decoded.tribulationCurrentStrike());
        assertEquals(3, decoded.tribulationTotalStrikes());
        assertEquals(40, decoded.tribulationNextStrikeTicks());
    }

    @Test
    void coreFormationTribulationFailureClearsGoldCoreAndFallsBackOneStage() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.CORE_FORMATION, RealmStage.EARLY, 0));
        cultivation.setGoldCore(GoldCoreGrade.HIGH, 82);
        cultivation.startTribulation(Realm.CORE_FORMATION, 3, 0);

        cultivation.failTribulationPenalty();

        assertFalse(cultivation.isTribulationActive());
        assertEquals(GoldCoreGrade.NONE, cultivation.getGoldCoreGrade());
        assertEquals(0, cultivation.getGoldCoreScore());
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, cultivation.getRealm());
        assertEquals(RealmStage.PEAK, cultivation.getStage());
    }

    @Test
    void nascentSoulTribulationFailureKeepsExistingGoldCore() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.NASCENT_SOUL, RealmStage.EARLY, 0));
        cultivation.setGoldCore(GoldCoreGrade.PERFECT, 96);
        cultivation.startTribulation(Realm.NASCENT_SOUL, 5, 0);

        cultivation.failTribulationPenalty();

        assertFalse(cultivation.isTribulationActive());
        assertEquals(GoldCoreGrade.PERFECT, cultivation.getGoldCoreGrade());
        assertEquals(96, cultivation.getGoldCoreScore());
        assertEquals(Realm.CORE_FORMATION, cultivation.getRealm());
        assertEquals(RealmStage.LATE, cultivation.getStage());
    }

    @Test
    void learnedTechniquePacketRoundTripsAndRejectsOversizedPayloads() {
        SyncLearnedTechniquesPacket packet = new SyncLearnedTechniquesPacket(
                List.of("fireball", "earth_wall"),
                List.of("fireball", "", "earth_wall", "", "", "", ""),
                Map.of("fireball", 42));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncLearnedTechniquesPacket.encode(packet, buffer);
        SyncLearnedTechniquesPacket decoded = SyncLearnedTechniquesPacket.decode(buffer);

        assertEquals(packet.learnedTechniques(), decoded.learnedTechniques());
        assertEquals(packet.techniqueSlots(), decoded.techniqueSlots());
        assertEquals(packet.cooldownRemainingTicks(), decoded.cooldownRemainingTicks());

        FriendlyByteBuf tooManyLearned = new FriendlyByteBuf(Unpooled.buffer());
        tooManyLearned.writeVarInt(SyncLearnedTechniquesPacket.MAX_LEARNED_TECHNIQUES + 1);
        assertThrows(RuntimeException.class, () -> SyncLearnedTechniquesPacket.decode(tooManyLearned));

        FriendlyByteBuf tooManySlots = new FriendlyByteBuf(Unpooled.buffer());
        tooManySlots.writeVarInt(0);
        tooManySlots.writeVarInt(PlayerCultivation.TECHNIQUE_SLOT_COUNT + 1);
        assertThrows(RuntimeException.class, () -> SyncLearnedTechniquesPacket.decode(tooManySlots));

        FriendlyByteBuf tooManyCooldowns = new FriendlyByteBuf(Unpooled.buffer());
        tooManyCooldowns.writeVarInt(0);
        tooManyCooldowns.writeVarInt(0);
        tooManyCooldowns.writeVarInt(SyncLearnedTechniquesPacket.MAX_COOLDOWNS + 1);
        assertThrows(RuntimeException.class, () -> SyncLearnedTechniquesPacket.decode(tooManyCooldowns));

        FriendlyByteBuf oversizedId = new FriendlyByteBuf(Unpooled.buffer());
        String longTechniqueId = "x".repeat(SyncLearnedTechniquesPacket.MAX_TECHNIQUE_ID_LENGTH + 1);
        oversizedId.writeVarInt(1);
        oversizedId.writeUtf(longTechniqueId, longTechniqueId.length());
        assertThrows(RuntimeException.class, () -> SyncLearnedTechniquesPacket.decode(oversizedId));
    }

    @Test
    void setTechniqueSlotPacketRoundTripsAndRejectsOversizedTechniqueId() {
        SetTechniqueSlotPacket packet = new SetTechniqueSlotPacket(2, "fireball");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SetTechniqueSlotPacket.encode(packet, buffer);
        SetTechniqueSlotPacket decoded = SetTechniqueSlotPacket.decode(buffer);

        assertEquals(packet.slot(), decoded.slot());
        assertEquals(packet.techniqueId(), decoded.techniqueId());

        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        String longTechniqueId = "x".repeat(SetTechniqueSlotPacket.MAX_TECHNIQUE_ID_LENGTH + 1);
        oversized.writeVarInt(0);
        oversized.writeUtf(longTechniqueId, longTechniqueId.length());

        assertThrows(RuntimeException.class, () -> SetTechniqueSlotPacket.decode(oversized));
    }

    @Test
    void validatesTribulationWaveAndTriggerRules() {
        // Aligned to 文本材料/data/tribulation_rules.json waves (minor=3, major=5, void=7, great-vehicle+=9)
        assertEquals(3, TribulationService.getStrikeCount(Realm.CORE_FORMATION));
        assertEquals(3, TribulationService.getStrikeCount(Realm.NASCENT_SOUL));
        assertEquals(5, TribulationService.getStrikeCount(Realm.SOUL_TRANSFORMATION));
        assertEquals(7, TribulationService.getStrikeCount(Realm.VOID_REFINEMENT));
        assertEquals(9, TribulationService.getStrikeCount(Realm.UNITY));
        assertEquals(9, TribulationService.getStrikeCount(Realm.MAHAYANA));
        assertEquals(9, TribulationService.getStrikeCount(Realm.TRIBULATION));
        assertEquals(9, TribulationService.getStrikeCount(Realm.TRUE_IMMORTAL));

        assertFalse(TribulationService.shouldTriggerAfterBreakthrough(Realm.QI_REFINING, Realm.FOUNDATION_ESTABLISHMENT));
        assertFalse(TribulationService.shouldTriggerAfterBreakthrough(Realm.FOUNDATION_ESTABLISHMENT, Realm.FOUNDATION_ESTABLISHMENT));
        assertTrue(TribulationService.shouldTriggerAfterBreakthrough(Realm.FOUNDATION_ESTABLISHMENT, Realm.CORE_FORMATION));
    }

    @Test
    void validatesTribulationDamageFormulaInputsAndCap() {
        int coreRequirement = TribulationService.getTargetDivineSenseRequirement(Realm.CORE_FORMATION);
        double base = TribulationService.calculateDamageReductionPercent(0, 0, coreRequirement, Realm.CORE_FORMATION, 1.0D, 0);
        double body = TribulationService.calculateDamageReductionPercent(0, 500, coreRequirement, Realm.CORE_FORMATION, 1.0D, 0);
        double sense = TribulationService.calculateDamageReductionPercent(0, 0, coreRequirement * 3, Realm.CORE_FORMATION, 1.0D, 0);
        double leyline = TribulationService.calculateDamageReductionPercent(0, 0, coreRequirement, Realm.CORE_FORMATION, 4.0D, 100);
        double capped = TribulationService.calculateDamageReductionPercent(90, 1000000, Integer.MAX_VALUE, Realm.TRUE_IMMORTAL, 5.0D, 1000);

        assertTrue(body > base);
        assertTrue(sense > base);
        assertTrue(leyline > base);
        assertEquals(0.95D, capped, 0.0001D);

        double firstCoreStrike = TribulationService.calculateStrikeDamage(100.0D, Realm.CORE_FORMATION, 1, 3,
                0, 0, coreRequirement, 1.0D, 0);
        double protectedCoreStrike = TribulationService.calculateStrikeDamage(100.0D, Realm.CORE_FORMATION, 1, 3,
                50, 500, coreRequirement * 2, 3.0D, 100);
        assertTrue(firstCoreStrike > protectedCoreStrike);
        assertTrue(protectedCoreStrike >= 1.0D);
    }

    private static void assertPhase1Baseline(Realm realm, int manaBase, int divSenseBase, int hpBase, int cultivationMaxSpan) {
        assertEquals(manaBase, RealmStageConfig.getManaBase(realm));
        assertEquals(divSenseBase, RealmStageConfig.getDivSenseBase(realm));
        assertEquals(hpBase, RealmStageConfig.getHpBase(realm));
        assertEquals(cultivationMaxSpan, realm.getStageExpSpan());
    }

    private static int addMeditationGainForWorldpackEvent(String eventId, MeditationFormula.Breakdown breakdown) {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 0));
        cultivation.setWorldpackDailyEvent(eventId, 24000L);
        return cultivation.addMeditationCultivation(breakdown);
    }

    private static CompoundTag realmTag(Realm realm, RealmStage stage, int cultivation) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Realm", realm.name());
        tag.putString("Stage", stage.name());
        tag.putLong("cultivation", cultivation);
        tag.putInt("mana", 100);
        tag.putInt("divSense", 5);
        tag.putString("SpiritualRoot", SpiritualRoot.HEAVENLY.name());
        return tag;
    }
}
