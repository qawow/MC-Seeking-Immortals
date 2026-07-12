package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SkillTypeDataTest {
    @Test
    void bigDipperCooldownMatchesServerEffectAndClientJson() throws Exception {
        assertEquals(80, SkillType.BIG_DIPPER_SWORD_ARRAY.getConfiguredSpiritualPowerCost());
        assertEquals(160, SkillType.BIG_DIPPER_SWORD_ARRAY.getConfiguredCooldownTicks());

        JsonObject entry = findTechnique("big_dipper_sword_array");
        assertNotNull(entry);
        assertEquals(80, entry.get("cost").getAsInt());
        assertEquals(160, entry.get("cooldown_ticks").getAsInt());
    }

    private static JsonObject findTechnique(String id) throws Exception {
        Path path = Path.of("src/main/resources/data/seeking_immortals/cultivation/foundation_establishment_techniques.json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonArray techniques = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("techniques");
            for (int i = 0; i < techniques.size(); i++) {
                JsonObject object = techniques.get(i).getAsJsonObject();
                if (id.equals(object.get("id").getAsString())) {
                    return object;
                }
            }
        }
        return null;
    }
}
