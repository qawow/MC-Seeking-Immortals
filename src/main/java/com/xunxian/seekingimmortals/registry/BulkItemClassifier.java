package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BulkItemClassifier {
    private static final String ALCHEMY_RECIPES_RESOURCE =
            "data/seeking_immortals/text_material/alchemy_recipes.json";
    private static final Set<String> NON_CONSUMABLE_PILL_IDS = Set.of("spirit_pill_voucher");
    private static final Set<String> EXECUTABLE_CONSUMABLE_IDS = Set.of(
            "spirit_rice_bowl",
            "spirit_wine_jar",
            "beast_meat_jerky",
            "tribulation_ward_charm",
            "spirit_beast_feed",
            "low_spirit_stone_pouch",
            "nascent_soul_escape_charm",
            "spirit_root_test_stone",
            "recipe_scroll_bundle",
            "talisman_crate_low",
            "talisman_crate_mid",
            "talisman_crate_high",
            "storage_pouch_low",
            "storage_pouch_mid",
            "spirit_stone_pouch_sealed",
            "pirate_loot_bundle",
            "yin_body_protection_charm",
            "minor_thunder_ward_charm",
            "major_thunder_array_token",
            "spirit_garden_fertilizer",
            "yanyue_incense_bundle",
            "inverse_star_smuggler_pack",
            "spirit_stone_mid_bulk",
            "spirit_stone_high_bulk",
            "escape_talisman",
            "fireball_talisman",
            "ice_shard_talisman",
            "golden_armor_talisman",
            "beast_feed_spirit",
            "storage_bag_low",
            "storage_bag_high",
            "spirit_water_flask",
            "healing_salve",
            "poison_antidote_pack",
            "smoke_bomb_spirit",
            "sound_beacon",
            "detox_minor_pill",
            "talisman_ink_bottle",
            "spirit_sand_pouch",
            "yin_coffin_nail",
            "wind_feather_raft_blueprint"
    );
    private static final Map<String, AlchemyFormulaSource> ALCHEMY_FORMULA_SOURCES =
            loadAlchemyFormulaSources();

    private BulkItemClassifier() {}

    public record ConsumableDefinition(
            String id,
            String display,
            String category,
            String realmMin,
            String effect,
            int storageSlots
    ) {}

    public static BulkItemKind classify(String id, String category) {
        String key = normalize(id);
        if (key.isBlank()) {
            return BulkItemKind.CARRIER;
        }
        if (recipeOutput(key).isPresent()) {
            return BulkItemKind.FORMULA;
        }
        if (ArtifactDataService.builtin().findArtifact(key)
                .filter(BulkItemClassifier::isRuntimeArtifact)
                .isPresent()) {
            return BulkItemKind.ARTIFACT;
        }
        if (!NON_CONSUMABLE_PILL_IDS.contains(key)
                && PillEffectCatalog.findByPillId(key).isPresent()) {
            return BulkItemKind.PILL;
        }
        if (consumable(key).isPresent()) {
            return BulkItemKind.CONSUMABLE;
        }
        return BulkItemKind.CARRIER;
    }

    public static Optional<String> recipeOutput(String id) {
        String key = normalize(id);
        ExtendedCatalogService.IdDisplay entry = ExtendedCatalogService.builtin().alchemyRecipes().get(key);
        if (entry == null || entry.extra() == null || entry.extra().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(normalize(entry.extra()));
    }

    public static AlchemyFormulaSource formulaSource(String id) {
        return ALCHEMY_FORMULA_SOURCES.getOrDefault(normalize(id), AlchemyFormulaSource.PAPER);
    }

    public static Optional<ConsumableDefinition> consumable(String id) {
        String key = normalize(id);
        if (!EXECUTABLE_CONSUMABLE_IDS.contains(key)) {
            return Optional.empty();
        }
        ExtendedCatalogService.ConsumableEntry entry =
                ExtendedCatalogService.builtin().consumables().get(key);
        if (entry == null) {
            return Optional.empty();
        }
        String effect = executableEffect(key, entry.effect());
        return Optional.of(new ConsumableDefinition(
                key,
                entry.display(),
                entry.category(),
                entry.realmMin(),
                effect,
                storageSlots(key, entry.effect())));
    }

    public static String basePillId(String id) {
        String path = normalize(id);
        for (String suffix : new String[]{"_mid", "_middle", "_high", "_supreme", "_perfect", "_low"}) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    private static String executableEffect(String id, String catalogEffect) {
        String effect = normalize(catalogEffect);
        int storageSlots = parseStorageSlots(effect);
        if (storageSlots == 9 || storageSlots == 18 || storageSlots == 27) {
            return "portable_storage_" + storageSlots;
        }
        String dedicated = switch (id) {
            case "detox_minor_pill" -> "detox_minor";
            case "talisman_ink_bottle" -> "talisman_craft_material";
            case "spirit_sand_pouch" -> "array_fuel";
            case "yin_coffin_nail" -> "corpse_control";
            case "wind_feather_raft_blueprint" -> "vehicle_craft";
            default -> "";
        };
        if (!dedicated.isBlank()) {
            return dedicated;
        }
        if (!effect.isBlank()) {
            return effect;
        }
        return switch (id) {
            case "talisman_crate_low" -> "random_talisman_low";
            case "talisman_crate_mid" -> "random_talisman_mid";
            case "talisman_crate_high" -> "random_talisman_high";
            case "inverse_star_smuggler_pack" -> "open_random_contraband";
            case "spirit_stone_mid_bulk" -> "spirit_stone_mid_bundle";
            case "spirit_stone_high_bulk" -> "spirit_stone_high_bundle";
            case "escape_talisman" -> "short_escape";
            case "fireball_talisman" -> "fireball_cast";
            case "ice_shard_talisman" -> "ice_shard_cast";
            case "golden_armor_talisman" -> "golden_armor";
            case "beast_feed_spirit" -> "pet_loyalty_plus";
            case "storage_bag_low" -> "portable_storage_9";
            case "storage_bag_high" -> "portable_storage_27";
            case "spirit_water_flask" -> "restore_spirit";
            case "healing_salve" -> "restore_health";
            case "poison_antidote_pack" -> "clear_poison";
            case "smoke_bomb_spirit" -> "smoke_screen";
            case "sound_beacon" -> "sound_beacon";
            default -> effect;
        };
    }

    private static int storageSlots(String id, String catalogEffect) {
        return switch (id) {
            case "storage_pouch_low", "storage_bag_low" -> 9;
            case "storage_pouch_mid" -> 18;
            case "storage_bag_high" -> 27;
            default -> parseStorageSlots(catalogEffect);
        };
    }

    private static int parseStorageSlots(String effect) {
        String normalized = normalize(effect);
        String prefix = "extra_inventory_slots_";
        if (!normalized.startsWith(prefix)) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(27, Integer.parseInt(normalized.substring(prefix.length()))));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isRuntimeArtifact(ArtifactDataService.ArtifactDefinition definition) {
        return !"material_artifact".equals(normalize(definition.type()));
    }

    private static Map<String, AlchemyFormulaSource> loadAlchemyFormulaSources() {
        Map<String, AlchemyFormulaSource> sources = new LinkedHashMap<>();
        try (InputStream stream = BulkItemClassifier.class.getClassLoader()
                .getResourceAsStream(ALCHEMY_RECIPES_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shipped alchemy recipe resource: "
                        + ALCHEMY_RECIPES_RESOURCE);
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                throw new IllegalStateException("Alchemy recipe resource must be a JSON object");
            }
            JsonArray recipes = root.getAsJsonObject().getAsJsonArray("recipes");
            if (recipes == null) {
                throw new IllegalStateException("Alchemy recipe resource is missing recipes array");
            }
            for (JsonElement element : recipes) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject recipe = element.getAsJsonObject();
                String id = normalize(recipe.has("id") ? recipe.get("id").getAsString() : "");
                if (id.isBlank()) {
                    continue;
                }
                String medium = normalize(recipe.has("medium") ? recipe.get("medium").getAsString() : "");
                sources.put(id, sourceForMedium(medium));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read shipped alchemy recipe resource", exception);
        }
        return Collections.unmodifiableMap(sources);
    }

    private static AlchemyFormulaSource sourceForMedium(String medium) {
        return switch (medium) {
            case "jade_slip" -> AlchemyFormulaSource.JADE;
            case "sect_secret_scroll" -> AlchemyFormulaSource.SECT_SECRET;
            case "", "paper_formula" -> AlchemyFormulaSource.PAPER;
            default -> throw new IllegalStateException("Unknown alchemy recipe medium: " + medium);
        };
    }
}
