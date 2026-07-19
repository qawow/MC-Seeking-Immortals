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
        assertEquals(114, PillEffectCatalog.size());

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
    void noShippedPillFallsBackToGenericCultivation() {
        for (PillEffectCatalog.Entry entry : PillEffectCatalog.all().values()) {
            assertNotEquals("generic_cultivation", entry.effect(), entry.pillId());
            assertFalse(entry.effect().isBlank(), entry.pillId());
        }
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
