package com.xunxian.seekingimmortals.skill.effect.spell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalismanConsumeSpellCorpusTest {
    @Test
    void authoredEffectKeysRouteToDeliberateModes() throws Exception {
        // Semantics-critical keys must not fall through to the damage-AOE default.
        assertEquals("MOVEMENT", TalismanConsumeSpell.classifyMode("mask_qi").name());
        assertEquals("BUFF", TalismanConsumeSpell.classifyMode("local_spirit_boost").name());
        assertEquals("CONTROL", TalismanConsumeSpell.classifyMode("prevent_teleport").name());
        assertEquals("CONTROL", TalismanConsumeSpell.classifyMode("soul_bind").name());
        assertEquals("MOVEMENT", TalismanConsumeSpell.classifyMode("long_escape").name());
        assertEquals("MOVEMENT", TalismanConsumeSpell.classifyMode("array_teleport").name());
        assertEquals("BUFF", TalismanConsumeSpell.classifyMode("auto_resurrect_once").name());
        assertEquals("BUFF", TalismanConsumeSpell.classifyMode("yin_damage_resist_0.3").name());
        assertEquals("BUFF", TalismanConsumeSpell.classifyMode("temp_wall").name());
        assertEquals("AOE", TalismanConsumeSpell.classifyMode("aoe_fire").name());
        assertEquals("AOE", TalismanConsumeSpell.classifyMode("thunder_strike").name());
        assertEquals("CONTROL", TalismanConsumeSpell.classifyMode("slow_ice").name());
    }

    @Test
    void everyCorpusEffectKeyIsAccountedFor() throws Exception {
        Set<String> known = new LinkedHashSet<>();
        Path dir = Path.of("src", "main", "resources", "data", "seeking_immortals",
                "text_material", "techniques");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                JsonArray arr = root.has("techniques") && root.get("techniques").isJsonArray()
                        ? root.getAsJsonArray("techniques") : new JsonArray();
                for (JsonElement element : arr) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = element.getAsJsonObject();
                    JsonObject effect = o.has("effect") && o.get("effect").isJsonObject()
                            ? o.getAsJsonObject("effect") : new JsonObject();
                    String key = effect.has("effect_key") ? effect.get("effect_key").getAsString()
                            : o.has("effect_key") ? o.get("effect_key").getAsString() : "";
                    if (!key.isBlank()) {
                        known.add(key.toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
        }
        assertTrue(known.size() >= 18, "corpus should expose the authored effect_key set, got " + known);
        // Generic marker key and every specific key must classify without throwing;
        // specific keys (besides the generic family markers) must not need PROJECTILE fallback surprises.
        for (String key : known) {
            TalismanConsumeSpell.Mode mode = TalismanConsumeSpell.classifyMode(key);
            assertTrue(mode != null, key);
            if (!key.equals("talisman_consume") && !key.startsWith("aoe")
                    && !key.equals("thunder_strike")) {
                assertTrue(mode != TalismanConsumeSpell.Mode.AOE,
                        "specific effect_key '" + key + "' must not fall through to default AOE");
            }
        }
    }
}
