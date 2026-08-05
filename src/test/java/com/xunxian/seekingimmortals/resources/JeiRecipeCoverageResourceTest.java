package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.craft.TalismanCraftService;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiRecipeCoverageResourceTest {
    private static final Path ALCHEMY_ROOT = Path.of(
            "src", "main", "resources", "data", "seeking_immortals", "alchemy", "recipes");
    private static final Path ALCHEMY_MANIFEST = ALCHEMY_ROOT.getParent().resolve("recipe_manifest.json");

    @Test
    void allThreeAuthoritativeRecipeCorporaKeepTheirExpectedCoverage() throws Exception {
        Set<String> alchemyIds = new LinkedHashSet<>();
        try (Stream<Path> paths = Files.list(ALCHEMY_ROOT)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonObject recipe = JsonParser.parseReader(reader).getAsJsonObject();
                    String id = recipe.get("id").getAsString();
                    assertTrue(alchemyIds.add(id), "Duplicate alchemy recipe id " + id);
                    JsonArray ingredients = recipe.getAsJsonArray("ingredients");
                    assertFalse(ingredients.isEmpty(), path + " has no JEI inputs");
                    for (JsonElement ingredient : ingredients) {
                        assertTrue(ingredient.getAsJsonObject().get("count").getAsInt() > 0,
                                path + " has a non-positive input count");
                    }
                    JsonObject outputs = recipe.has("output_items")
                            ? recipe.getAsJsonObject("output_items")
                            : recipe.getAsJsonObject("outputs");
                    assertEquals(4, outputs.size(), path + " must expose four quality outputs");
                }
            }
        }

        Set<String> manifestIds = new LinkedHashSet<>();
        try (Reader reader = Files.newBufferedReader(ALCHEMY_MANIFEST, StandardCharsets.UTF_8)) {
            JsonArray recipes = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("recipes");
            for (JsonElement recipe : recipes) {
                assertTrue(manifestIds.add(recipe.getAsString()),
                        "Duplicate packaged alchemy manifest id " + recipe.getAsString());
            }
        }

        // The three removed generic pill recipes must not re-enter the packaged JEI corpus.
        // Y-C: +peiying_dan, the coop route that finally closes peiying_material_hunt.
        assertEquals(127, alchemyIds.size());
        assertEquals(alchemyIds, manifestIds,
                "The client JEI packaged manifest must match every authoritative alchemy recipe file");
        assertEquals(73, ArtifactDataService.builtin().refinementRecipes().size());
        assertEquals(24, TalismanCraftService.recipeBlueprintCount());
    }
}
