package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletionAssetContractTest {
    @Test
    void compatibilityStationCarriersReuseCanonicalBlockModelsAndNames() throws Exception {
        Path models = Path.of("src", "main", "resources", "assets", "seeking_immortals", "models", "item");
        Path lang = Path.of("src", "main", "resources", "assets", "seeking_immortals", "lang");
        JsonObject zh = JsonParser.parseString(Files.readString(lang.resolve("zh_cn.json"))).getAsJsonObject();
        JsonObject en = JsonParser.parseString(Files.readString(lang.resolve("en_us.json"))).getAsJsonObject();

        for (String id : List.of("alchemy_furnace_g1", "earth_fire_alchemy_room",
                "refinement_forge_g1", "yin_essence_ore_block")) {
            JsonObject model = JsonParser.parseString(
                    Files.readString(models.resolve(id + ".json"))).getAsJsonObject();
            assertTrue(model.get("parent").getAsString().startsWith("seeking_immortals:block/"));
            assertTrue(zh.has("item.seeking_immortals." + id));
            assertTrue(en.has("item.seeking_immortals." + id));
        }
        assertEquals("seeking_immortals:block/alchemy_furnace",
                JsonParser.parseString(Files.readString(models.resolve("alchemy_furnace_g1.json")))
                        .getAsJsonObject().get("parent").getAsString());
    }
}
