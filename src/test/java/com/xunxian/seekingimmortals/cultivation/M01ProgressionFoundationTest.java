package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M01ProgressionFoundationTest {
    @Test
    void realmDesignIdsAndLifespanAlignWithCorpus() {
        assertEquals("FOUNDATION", Realm.FOUNDATION_ESTABLISHMENT.getDesignId());
        assertEquals("DEITY_TRANSFORMATION", Realm.SOUL_TRANSFORMATION.getDesignId());
        assertEquals("BODY_INTEGRATION", Realm.UNITY.getDesignId());
        assertEquals("GREAT_VEHICLE", Realm.MAHAYANA.getDesignId());
        assertEquals("TRIBULATION_LAND", Realm.TRIBULATION.getDesignId());

        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, Realm.fromDesignId("FOUNDATION"));
        assertEquals(Realm.SOUL_TRANSFORMATION, Realm.fromDesignId("DEITY_TRANSFORMATION"));
        assertEquals(Realm.UNITY, Realm.fromDesignId("BODY_INTEGRATION"));
        assertEquals(Realm.MAHAYANA, Realm.fromDesignId("GREAT_VEHICLE"));
        assertEquals(Realm.TRIBULATION, Realm.fromDesignId("TRIBULATION_LAND"));

        assertEquals(120, Realm.QI_REFINING.getLifespanYears());
        assertEquals(250, Realm.FOUNDATION_ESTABLISHMENT.getLifespanYears());
        assertEquals(600, Realm.CORE_FORMATION.getLifespanYears());
        assertEquals(1200, Realm.NASCENT_SOUL.getLifespanYears());
        assertEquals(2500, Realm.SOUL_TRANSFORMATION.getLifespanYears());
        assertEquals(5000, Realm.VOID_REFINEMENT.getLifespanYears());
        assertEquals(10000, Realm.UNITY.getLifespanYears());
        assertEquals(100000, Realm.MAHAYANA.getLifespanYears());
        assertEquals(13, Realm.QI_REFINING.getSubStages());
        assertEquals(4, Realm.FOUNDATION_ESTABLISHMENT.getSubStages());
        assertEquals(3, Realm.CORE_FORMATION.getSubStages());
    }

    @Test
    void spiritRootAttributesIncludeYinYangAndCorpusLookup() {
        assertEquals(SpiritualRootAttribute.YIN, SpiritualRootAttribute.fromCorpusId("yin"));
        assertEquals(SpiritualRootAttribute.YANG, SpiritualRootAttribute.fromCorpusId("yang"));
        assertEquals(SpiritualRootAttribute.FIRE, SpiritualRootAttribute.fromCorpusId("fire"));
        assertEquals("yin", SpiritualRootAttribute.YIN.getCorpusId());
        assertEquals("yang", SpiritualRootAttribute.YANG.getCorpusId());
    }

    @Test
    void constitutionCatalogAndPathRaceAreLoaded() {
        assertTrue(ConstitutionCatalogService.builtin().size() >= 10);
        assertTrue(ConstitutionCatalogService.builtin().find("tongyu_fengsui").isPresent());
        assertTrue(ConstitutionCatalogService.builtin().find("dragon_chant").isPresent());
        assertEquals(2.5D, ConstitutionCatalogService.STACK_CAP, 0.0001D);
        assertEquals(2.5D, ConstitutionCatalogService.clampStackedCultivation(5.0D, 1.15D), 0.0001D);

        assertTrue(PathRaceCatalog.builtin().isKnownRace("human_mortal"));
        assertTrue(PathRaceCatalog.builtin().isKnownRace("ghost_cultivator"));
        assertTrue(PathRaceCatalog.builtin().findGhostStage("yin_body").isPresent());
        assertTrue(PathRaceCatalog.builtin().isGhostPath("ghost"));
    }

    @Test
    void tribulationAndBreakthroughCatalogsDriveRuntimeValues() {
        assertEquals(3, TribulationRulesCatalog.strikeCount(Realm.NASCENT_SOUL));
        assertEquals(5, TribulationRulesCatalog.strikeCount(Realm.SOUL_TRANSFORMATION));
        assertEquals(9, TribulationRulesCatalog.strikeCount(Realm.VOID_REFINEMENT));
        assertEquals(12, TribulationRulesCatalog.strikeCount(Realm.UNITY));
        assertEquals(18, TribulationRulesCatalog.strikeCount(Realm.MAHAYANA));
        assertEquals(27, TribulationRulesCatalog.strikeCount(Realm.TRIBULATION));

        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_13, 0));
        // 炼气圆满跨境筑基 base_success=0.35
        assertEquals(0.35D, BreakthroughCatalog.baseSuccess(cultivation), 0.0001D);

        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_5, 0));
        // 炼气层内推进 base_success=0.85
        assertEquals(0.85D, BreakthroughCatalog.baseSuccess(cultivation), 0.0001D);
    }

    @Test
    void playerCultivationPersistsPathRaceConstitution() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.QI_REFINING, RealmStage.LAYER_1, 0));
        cultivation.setConstitutionId("tongyu_fengsui");
        cultivation.setCultivationPathId("ghost_cultivator");
        cultivation.setPlayableRaceId("ghost_cultivator");
        cultivation.setGhostPathStageId("yin_body");

        CompoundTag saved = cultivation.saveNBTData();
        PlayerCultivation reloaded = new PlayerCultivation();
        reloaded.loadNBTData(saved);

        assertEquals("tongyu_fengsui", reloaded.getConstitutionId());
        assertEquals(PathRaceCatalog.GHOST_PATH_ID, reloaded.getCultivationPathId());
        assertEquals("ghost_cultivator", reloaded.getPlayableRaceId());
        assertEquals("yin_body", reloaded.getGhostPathStageId());
        assertTrue(reloaded.isGhostPath());
        assertEquals(SpecialPhysique.JADE_PHOENIX_MARROW, reloaded.getSpecialPhysique());
    }

    @Test
    void progressionGateApiChecksRealmRootPathRace() {
        PlayerCultivation cultivation = new PlayerCultivation();
        cultivation.loadNBTData(realmTag(Realm.FOUNDATION_ESTABLISHMENT, RealmStage.EARLY, 0));
        cultivation.setSpiritualRoot(SpiritualRoot.HEAVENLY);
        cultivation.setSpiritualRootAttribute(SpiritualRootAttribute.FIRE);
        cultivation.setCultivationPathId("orthodox");
        cultivation.setPlayableRaceId("human_cultivator");
        cultivation.setConstitutionId("dragon_chant");

        assertTrue(ProgressionGateApi.meetsRealm(cultivation, "QI_REFINING"));
        assertTrue(ProgressionGateApi.meetsRealm(cultivation, "FOUNDATION"));
        assertFalse(ProgressionGateApi.meetsRealm(cultivation, "NASCENT_SOUL"));
        assertTrue(ProgressionGateApi.meetsRoot(cultivation, "HEAVENLY"));
        assertTrue(ProgressionGateApi.meetsRoot(cultivation, "fire"));
        assertFalse(ProgressionGateApi.meetsRoot(cultivation, "water"));
        assertTrue(ProgressionGateApi.meetsPath(cultivation, "orthodox"));
        assertFalse(ProgressionGateApi.meetsPath(cultivation, "ghost"));
        assertTrue(ProgressionGateApi.meetsRace(cultivation, "human"));
        assertTrue(ProgressionGateApi.meetsConstitution(cultivation, "dragon_chant"));
    }

    private static CompoundTag realmTag(Realm realm, RealmStage stage, int cultivation) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Realm", realm.name());
        tag.putString("Stage", stage.name());
        tag.putLong("cultivation", cultivation);
        tag.putInt("mana", 100);
        tag.putInt("divSense", 5);
        tag.putString("SpiritualRoot", SpiritualRoot.HEAVENLY.name());
        tag.putString("SpiritualRootAttributes", "FIRE");
        return tag;
    }
}
