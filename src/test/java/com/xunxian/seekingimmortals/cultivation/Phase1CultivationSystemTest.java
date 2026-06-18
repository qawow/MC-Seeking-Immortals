package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertPhase1Baseline(Realm.QI_REFINING, 100, 50, 20, 100);
        assertPhase1Baseline(Realm.FOUNDATION_ESTABLISHMENT, 250, 150, 40, 300);
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

        CompoundTag saved = cultivation.saveNBTData();
        PlayerCultivation loaded = new PlayerCultivation();
        loaded.loadNBTData(saved);

        assertEquals(cultivation.getMana(), loaded.getMana());
        assertEquals(cultivation.getDivSense(), loaded.getDivSense());
        assertEquals(cultivation.getBodyRef(), loaded.getBodyRef());
        assertEquals(cultivation.getQiDevRiskFloat(), loaded.getQiDevRiskFloat(), 0.0001F);
        assertEquals(cultivation.getTribResFloat(), loaded.getTribResFloat(), 0.0001F);
        assertEquals(cultivation.getCultivationLong(), loaded.getCultivationLong());
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
    void validatesDerivedAttributeCalculations() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.setSpiritualRoot(SpiritualRoot.HEAVENLY);
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 0));

        assertEquals(20, cultivation.getMaxHealthPoints());
        assertEquals(1.0F, cultivation.getManaRecoveryPerSecond(), 0.0001F);
        assertEquals(0.5F, cultivation.getCultivationGainPerSecond(), 0.0001F);
        assertEquals(5.0F, cultivation.getFlyingSpeed(), 0.0001F);
        assertTrue(cultivation.getMeleeAttackPower() > 0.0D);
        assertTrue(cultivation.getDefensePower() > 0.0D);
    }

    private static void assertPhase1Baseline(Realm realm, int manaBase, int divSenseBase, int hpBase, int cultivationMaxSpan) {
        assertEquals(manaBase, RealmStageConfig.getManaBase(realm));
        assertEquals(divSenseBase, RealmStageConfig.getDivSenseBase(realm));
        assertEquals(hpBase, RealmStageConfig.getHpBase(realm));
        assertEquals(cultivationMaxSpan, realm.getStageExpSpan());
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
