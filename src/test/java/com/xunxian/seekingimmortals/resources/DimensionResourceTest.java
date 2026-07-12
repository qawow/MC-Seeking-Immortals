package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionResourceTest {
    private static final Path DATA_ROOT = Path.of("src", "main", "resources", "data", "seeking_immortals");

    @Test
    void spiritRealmDimensionsUseShippedDimensionTypes() throws Exception {
        for (String dimensionId : List.of("tianyuan", "spirit_fengyuan")) {
            Path dimensionPath = DATA_ROOT.resolve(Path.of("dimension", dimensionId + ".json"));
            JsonObject dimension = readObject(dimensionPath);
            assertEquals("seeking_immortals:" + dimensionId, requiredString(dimension, "type", dimensionPath));

            Path typePath = DATA_ROOT.resolve(Path.of("dimension_type", dimensionId + ".json"));
            assertTrue(Files.exists(typePath), "Missing dimension type for " + dimensionId);
            JsonObject type = readObject(typePath);
            assertSpiritRealmTypeShape(type, typePath);
        }
    }

    @Test
    void yinUnderworldPocketDimensionsUseShippedDimensionTypes() throws Exception {
        for (String dimensionId : List.of("yin_ming_pocket", "nether_river_pocket")) {
            Path dimensionPath = DATA_ROOT.resolve(Path.of("dimension", dimensionId + ".json"));
            JsonObject dimension = readObject(dimensionPath);
            assertEquals("seeking_immortals:" + dimensionId, requiredString(dimension, "type", dimensionPath));

            Path typePath = DATA_ROOT.resolve(Path.of("dimension_type", dimensionId + ".json"));
            assertTrue(Files.exists(typePath), "Missing dimension type for " + dimensionId);
            JsonObject type = readObject(typePath);
            assertYinPocketTypeShape(type, typePath);
        }
    }

    @Test
    void demonRiftDimensionUsesShippedEventType() throws Exception {
        Path dimensionPath = DATA_ROOT.resolve(Path.of("dimension", "demon_rift.json"));
        JsonObject dimension = readObject(dimensionPath);
        assertEquals("seeking_immortals:demon_rift", requiredString(dimension, "type", dimensionPath));

        Path typePath = DATA_ROOT.resolve(Path.of("dimension_type", "demon_rift.json"));
        assertTrue(Files.exists(typePath), "Missing dimension type for demon_rift");
        JsonObject type = readObject(typePath);
        assertDemonRiftTypeShape(type, typePath);
    }

    private static void assertSpiritRealmTypeShape(JsonObject type, Path path) {
        assertEquals(false, requiredBoolean(type, "ultrawarm", path));
        assertEquals(true, requiredBoolean(type, "natural", path));
        assertEquals(true, requiredBoolean(type, "has_skylight", path));
        assertEquals(false, requiredBoolean(type, "has_ceiling", path));
        assertEquals(true, requiredBoolean(type, "bed_works", path));
        assertEquals(false, requiredBoolean(type, "respawn_anchor_works", path));
        assertEquals(384, requiredInt(type, "logical_height", path));
        assertEquals(-64, requiredInt(type, "min_y", path));
        assertEquals(384, requiredInt(type, "height", path));
        assertEquals("#minecraft:infiniburn_overworld", requiredString(type, "infiniburn", path));
        assertEquals("minecraft:overworld", requiredString(type, "effects", path));
        assertTrue(type.has("monster_spawn_light_level"), path + " must define monster_spawn_light_level");
        assertTrue(type.has("monster_spawn_block_light_limit"), path + " must define monster_spawn_block_light_limit");
    }

    private static void assertYinPocketTypeShape(JsonObject type, Path path) {
        assertEquals(false, requiredBoolean(type, "ultrawarm", path));
        assertEquals(false, requiredBoolean(type, "natural", path));
        assertEquals(false, requiredBoolean(type, "has_skylight", path));
        assertEquals(true, requiredBoolean(type, "has_ceiling", path));
        assertEquals(false, requiredBoolean(type, "bed_works", path));
        assertEquals(false, requiredBoolean(type, "respawn_anchor_works", path));
        assertEquals(false, requiredBoolean(type, "has_raids", path));
        assertEquals(384, requiredInt(type, "logical_height", path));
        assertEquals(-64, requiredInt(type, "min_y", path));
        assertEquals(384, requiredInt(type, "height", path));
        assertEquals("#minecraft:infiniburn_overworld", requiredString(type, "infiniburn", path));
        assertEquals("minecraft:the_nether", requiredString(type, "effects", path));
        assertTrue(type.has("monster_spawn_light_level"), path + " must define monster_spawn_light_level");
        assertTrue(type.has("monster_spawn_block_light_limit"), path + " must define monster_spawn_block_light_limit");
    }

    private static void assertDemonRiftTypeShape(JsonObject type, Path path) {
        assertEquals(false, requiredBoolean(type, "ultrawarm", path));
        assertEquals(false, requiredBoolean(type, "natural", path));
        assertEquals(false, requiredBoolean(type, "has_skylight", path));
        assertEquals(false, requiredBoolean(type, "has_ceiling", path));
        assertEquals(false, requiredBoolean(type, "bed_works", path));
        assertEquals(false, requiredBoolean(type, "respawn_anchor_works", path));
        assertEquals(false, requiredBoolean(type, "has_raids", path));
        assertEquals(384, requiredInt(type, "logical_height", path));
        assertEquals(-64, requiredInt(type, "min_y", path));
        assertEquals(384, requiredInt(type, "height", path));
        assertEquals("#minecraft:infiniburn_overworld", requiredString(type, "infiniburn", path));
        assertEquals("minecraft:the_nether", requiredString(type, "effects", path));
        assertTrue(type.has("monster_spawn_light_level"), path + " must define monster_spawn_light_level");
        assertTrue(type.has("monster_spawn_block_light_limit"), path + " must define monster_spawn_block_light_limit");
    }

    private static JsonObject readObject(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertTrue(element.isJsonObject(), path + " must have a JSON object root");
            return element.getAsJsonObject();
        }
    }

    private static JsonElement requiredElement(JsonObject object, String field, Path path) {
        assertTrue(object.has(field), path + " missing required field " + field);
        return object.get(field);
    }

    private static String requiredString(JsonObject object, String field, Path path) {
        return requiredElement(object, field, path).getAsString();
    }

    private static boolean requiredBoolean(JsonObject object, String field, Path path) {
        return requiredElement(object, field, path).getAsBoolean();
    }

    private static int requiredInt(JsonObject object, String field, Path path) {
        return requiredElement(object, field, path).getAsInt();
    }
}
