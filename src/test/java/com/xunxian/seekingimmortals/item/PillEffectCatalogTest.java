package com.xunxian.seekingimmortals.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PillEffectCatalogTest {
    @Test
    void mergesRuntimeEffectsWithPillDesignMetadata() {
        assertEquals(113, PillEffectCatalog.size());

        PillEffectCatalog.Entry fire = PillEffectCatalog.findByPillId("huoyuan_pill").orElseThrow();
        assertEquals("spirit_gain_flat", fire.effect());
        assertEquals(50, fire.spiritGainFlat());
        assertEquals("fire", fire.element());
        assertEquals("QI_REFINING", fire.realmMin());

        PillEffectCatalog.Entry foundation = PillEffectCatalog.findByPillId("foundation_pill").orElseThrow();
        assertEquals("targeted_breakthrough_aid", foundation.effect());
        assertEquals("QI_REFINING", foundation.realmMin());
        assertEquals("FOUNDATION", foundation.realmTarget());

        PillEffectCatalog.Entry deathSubstitute = PillEffectCatalog.findByPillId("tihu_pill").orElseThrow();
        assertEquals("death_substitute_once", deathSubstitute.effect());
        assertTrue(deathSubstitute.effectTags().contains("death_substitute_once"));

        PillEffectCatalog.Entry pressure = PillEffectCatalog.findByPillId("pressure_resist_pill").orElseThrow();
        assertEquals("diyuan_adaptation", pressure.effect());
        assertEquals("VOID_REFINING", pressure.realmMin());
        assertTrue(pressure.effectTags().contains("diyuan_debuff_reduce"));
    }

    @Test
    void namedCultivationPillsResolveToDistinctEffects() {
        assertEquals("jade_spirit_tonic",
                PillEffectCatalog.findByPillId("biyu_pill").orElseThrow().effect());
        assertEquals("yuan_gathering",
                PillEffectCatalog.findByPillId("juyuan_pill").orElseThrow().effect());
        assertEquals("dragon_tiger_temper",
                PillEffectCatalog.findByPillId("longhu_pill").orElseThrow().effect());
        assertEquals("spirit_gather_tonic",
                PillEffectCatalog.findByPillId("spirit_condense_minor").orElseThrow().effect());
        assertEquals("spirit_gather_tonic",
                PillEffectCatalog.findByPillId("juling_pill").orElseThrow().effect());
        assertEquals("jiangchen_breakthrough_aid",
                PillEffectCatalog.findByPillId("jiangying_pill").orElseThrow().effect());
        assertEquals("jiangchen_pill", PillEffectCatalog.canonicalPillId("jiangying_pill"));
        assertEquals("yin_yang_balance",
                PillEffectCatalog.findByPillId("yin_yang_pill").orElseThrow().effect());
        assertEquals("spirit_seed_growth",
                PillEffectCatalog.findByPillId("spirit_seed_pill").orElseThrow().effect());
        assertEquals("star_sea_voyage",
                PillEffectCatalog.findByPillId("star_sea_pill").orElseThrow().effect());
        assertEquals("merit_tonic",
                PillEffectCatalog.findByPillId("tianyuan_merit_pill").orElseThrow().effect());
        assertEquals("realm_cultivation_aid",
                PillEffectCatalog.findByPillId("cultivation_aid_foundation").orElseThrow().effect());
        assertEquals("realm_cultivation_aid",
                PillEffectCatalog.findByPillId("cultivation_aid_nascent_soul").orElseThrow().effect());
        assertEquals("FOUNDATION",
                PillEffectCatalog.findByPillId("cultivation_aid_foundation").orElseThrow().realmTarget());
    }

    @Test
    void legacyGenericPillsAreRemovedWhileCanonicalReplacementsStayDistinct() {
        // The old three registry ids no longer represent shipped items.  Their NBT/formula
        // names remain accepted only by AlchemyFormulaKnowledge for save compatibility.
        assertTrue(PillEffectCatalog.findByPillId("qi_recovery_pill").isEmpty());
        assertTrue(PillEffectCatalog.findByPillId("cultivation_pill").isEmpty());
        assertTrue(PillEffectCatalog.findByPillId("breakthrough_pill").isEmpty());

        PillEffectCatalog.Entry recovery = PillEffectCatalog.findByPillId("spirit_recovery_pill").orElseThrow();
        PillEffectCatalog.Entry cultivation = PillEffectCatalog.findByPillId("cultivate_speed_pill").orElseThrow();
        PillEffectCatalog.Entry foundationAid = PillEffectCatalog.findByPillId("jiangchen_pill").orElseThrow();
        assertEquals("restore_mana_50pct", recovery.effect());
        assertEquals("cultivation_speed_1h", cultivation.effect());
        assertEquals("jiangchen_breakthrough_aid", foundationAid.effect());
        assertNotEquals(recovery.effect(), cultivation.effect());
        assertNotEquals(cultivation.effect(), foundationAid.effect());
        assertNotEquals("cultivation_speed_1h",
                PillEffectCatalog.findByPillId("tianyuan_spirit_pill").orElseThrow().effect());
    }

    @Test
    void noShippedPillFallsBackToGenericCultivation() {
        int cultivationProgress = 0;
        for (PillEffectCatalog.Entry entry : PillEffectCatalog.all().values()) {
            assertNotEquals("generic_cultivation", entry.effect(), entry.pillId());
            assertFalse(entry.effect().isBlank(), entry.pillId());
            if ("cultivation_progress".equals(entry.effect())) {
                cultivationProgress++;
            }
        }
        // Remaining true progress leftovers should stay rare after named splits.
        assertTrue(cultivationProgress <= 8, "cultivation_progress leftovers=" + cultivationProgress);
    }

    @Test
    void everyRuntimeEffectHasBothLanguageDescriptions() throws Exception {
        JsonObject zh = loadLanguage("zh_cn.json");
        JsonObject en = loadLanguage("en_us.json");
        for (PillEffectCatalog.Entry entry : PillEffectCatalog.all().values()) {
            String key = "tooltip.seeking_immortals.catalog_pill.effect." + entry.effect();
            assertTrue(zh.has(key), "missing zh key " + key);
            assertTrue(en.has(key), "missing en key " + key);
        }
    }

    @Test
    void deathSubstituteAndHeartDemonReductionPersistInCapabilityState() {
        PlayerCultivation cultivation = new PlayerCultivation();
        assertTrue(cultivation.grantDeathSubstitute());
        assertFalse(cultivation.grantDeathSubstitute());

        cultivation.applyHeartDemon(RandomSource.create());
        cultivation.increaseHeartDemonLayer(RandomSource.create());
        assertEquals(2, cultivation.getHeartDemonLevel());
        assertTrue(cultivation.reduceHeartDemon(1));
        assertEquals(1, cultivation.getHeartDemonLevel());

        PlayerCultivation restored = new PlayerCultivation();
        restored.loadNBTData(cultivation.saveNBTData());
        assertTrue(restored.hasDeathSubstituteReady());
        assertEquals(1, restored.getHeartDemonLevel());
        assertTrue(restored.consumeDeathSubstitute());
        assertFalse(restored.hasDeathSubstituteReady());
    }

    private static JsonObject loadLanguage(String file) throws Exception {
        try (InputStream stream = PillEffectCatalogTest.class.getClassLoader()
                     .getResourceAsStream("assets/seeking_immortals/lang/" + file)) {
            if (stream == null) throw new IllegalStateException("missing language file " + file);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
