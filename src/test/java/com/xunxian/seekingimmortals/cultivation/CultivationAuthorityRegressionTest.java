package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationAuthorityRegressionTest {
    @Test
    void nonBlankUnknownRealmsFailClosedWhileStageSuffixesResolve() {
        PlayerCultivation cultivation = new PlayerCultivation();

        assertEquals(Realm.MAHAYANA, Realm.fromDesignId("GREAT_VEHICLE_peak"));
        assertFalse(ProgressionGateApi.meetsRealm(cultivation, "GOLDEN_IMMORTAL"));
        assertFalse(WorldpackGameplayService.meetsMinRealm(Realm.TRUE_IMMORTAL, "GOLDEN_IMMORTAL"));
        assertTrue(WorldpackGameplayService.meetsMinRealm(Realm.MORTAL, ""));
    }

    @Test
    void techniqueCooldownsRoundTripOnlyWithGlobalTimeNbtVersion() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.setTechniqueCooldown("fireball_art", 123456L);

        CompoundTag saved = cultivation.saveNBTData();
        assertEquals(1, saved.getInt("CultivationNbtVersion"));

        PlayerCultivation loaded = new PlayerCultivation();
        loaded.loadNBTData(saved);
        assertEquals(123456L, loaded.getTechniqueCooldownUntilTick("fireball_art"));

        CompoundTag legacy = saved.copy();
        legacy.remove("CultivationNbtVersion");
        PlayerCultivation migrated = new PlayerCultivation();
        migrated.loadNBTData(legacy);
        assertEquals(0L, migrated.getTechniqueCooldownUntilTick("fireball_art"));
    }

    @Test
    void highRealmCapsSaturateInsteadOfOverflowing() {
        PlayerCultivation cultivation = new PlayerCultivation();
        CompoundTag tag = cultivation.saveNBTData();
        tag.putString("Realm", Realm.TRUE_IMMORTAL.name());
        tag.putString("Stage", RealmStage.LATE.name());
        tag.putString("GoldCoreGrade", GoldCoreGrade.PERFECT.name());
        cultivation.loadNBTData(tag);

        assertEquals(Integer.MAX_VALUE, cultivation.getMaxSpiritualPower());
        assertEquals(Integer.MAX_VALUE, cultivation.getMaxDivineConsciousness());
        assertEquals(Integer.MAX_VALUE, cultivation.getMaxHealthPoints());
        assertTrue(cultivation.getSpiritualPower() <= cultivation.getMaxSpiritualPower());
        assertTrue(cultivation.getDivineConsciousness() <= cultivation.getMaxDivineConsciousness());
    }

    @Test
    void realmStageCountsFollowPublishedSubStageContract() {
        assertEquals(1, PlayerCultivation.getStagesForRealmPublic(Realm.TRIBULATION).length);
        assertEquals(RealmStage.EARLY, PlayerCultivation.getStagesForRealmPublic(Realm.TRIBULATION)[0]);
    }
}
