package com.xunxian.seekingimmortals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify that high-value currency tokens with existing CatalogConsumableService handlers
 * now have non-empty effect fields in consumables_index.json (0.2.112 quick wins).
 */
public class CurrencyTokenEffectWiringTest {
    private static final String[] QUICK_WIN_TOKENS = {
        "spirit_boat_ticket",
        "void_palace_map_fragment",
        "fallen_demon_scout_report",
        "kunwu_map_scroll",
        "teleport_talisman_chaotic_sea",
        "star_palace_patrol_seal",
        "auction_invitation",
        "sect_identity_token"
    };

    private static final String[] EXPECTED_EFFECTS = {
        "travel_spirit_boat",
        "discover_void_palace",
        "discover_fallen_demon",
        "discover_kunwu",
        "travel_chaotic_sea",
        "star_palace_patrol",
        "open_auction_invite",
        "show_sect_identity"
    };

    @Test
    public void quickWinTokensHaveEffects() throws Exception {
        Path catalogPath = Paths.get("src/main/resources/data/seeking_immortals/catalog/consumables_index.json");
        assertTrue(catalogPath.toFile().exists(), "consumables_index.json must exist");

        JsonObject root = JsonParser.parseReader(new FileReader(catalogPath.toFile())).getAsJsonObject();
        JsonArray consumables = root.getAsJsonArray("consumables");
        assertNotNull(consumables, "consumables array must exist");

        List<String> missingEffects = new ArrayList<>();
        for (int i = 0; i < QUICK_WIN_TOKENS.length; i++) {
            String tokenId = QUICK_WIN_TOKENS[i];
            String expectedEffect = EXPECTED_EFFECTS[i];

            JsonObject entry = findConsumableById(consumables, tokenId);
            assertNotNull(entry, "Token '" + tokenId + "' must exist in consumables_index.json");

            String actualEffect = getEffectString(entry);
            if (actualEffect == null || actualEffect.isBlank()) {
                missingEffects.add(tokenId + " (expected: " + expectedEffect + ")");
            } else if (!expectedEffect.equals(actualEffect)) {
                fail("Token '" + tokenId + "' has effect '" + actualEffect + "' but expected '" + expectedEffect + "'");
            }
        }

        assertTrue(missingEffects.isEmpty(),
                "The following quick-win tokens still have empty effect fields: " + String.join(", ", missingEffects));
    }

    @Test
    public void allQuickWinEffectsMatchHandlers() throws Exception {
        // Verify that the effect strings we're using are actually handled in CatalogConsumableService
        Path servicePath = Paths.get("src/main/java/com/xunxian/seekingimmortals/item/CatalogConsumableService.java");
        assertTrue(servicePath.toFile().exists(), "CatalogConsumableService.java must exist");

        String serviceContent = java.nio.file.Files.readString(servicePath);

        List<String> missingHandlers = new ArrayList<>();
        for (String effect : EXPECTED_EFFECTS) {
            // Check if the effect string appears in a case statement
            if (!serviceContent.contains("case \"" + effect + "\"")) {
                missingHandlers.add(effect);
            }
        }

        assertTrue(missingHandlers.isEmpty(),
                "The following effects have no handler in CatalogConsumableService: " + String.join(", ", missingHandlers));
    }

    private JsonObject findConsumableById(JsonArray consumables, String id) {
        for (JsonElement elem : consumables) {
            JsonObject obj = elem.getAsJsonObject();
            if (id.equals(obj.get("id").getAsString())) {
                return obj;
            }
        }
        return null;
    }

    private String getEffectString(JsonObject entry) {
        if (!entry.has("effect")) {
            return null;
        }
        JsonElement effectElem = entry.get("effect");
        if (effectElem.isJsonPrimitive()) {
            return effectElem.getAsString();
        } else if (effectElem.isJsonArray()) {
            JsonArray arr = effectElem.getAsJsonArray();
            if (arr.size() > 0) {
                return arr.get(0).getAsString();
            }
        }
        return null;
    }
}
