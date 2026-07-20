package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
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
    private static final Set<String> NON_CONSUMABLE_PILL_IDS = Set.of();
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
            "wind_feather_raft_blueprint",
            "sect_contribution_token",
            "spirit_boat_ticket",
            "ferry_pass",
            "teleport_talisman_chaotic_sea",
            "diyuan_access_token",
            "spirit_gathering_array_disk",
            "auction_invitation",
            "sect_identity_token",
            "star_palace_tax_receipt",
            "star_palace_patrol_seal",
            "void_palace_map_fragment",
            "fallen_demon_scout_report",
            "kunwu_map_scroll",
            "demon_qi_purge_pill",
            "mortal_medicine",
            "jade_slip_blank",
            "paper_formula_scroll",
            "spirit_pill_voucher"
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
        if (isCatalogManual(key, category)) {
            return BulkItemKind.MANUAL;
        }
        if (isExecutableTalisman(key, category)) {
            return BulkItemKind.TALISMAN;
        }
        if (isEquipment(key, category)) {
            return BulkItemKind.EQUIPMENT;
        }
        return BulkItemKind.CARRIER;
    }

    public static boolean isEquipment(String id, String category) {
        String key = normalize(id);
        if (key.isBlank()) {
            return false;
        }
        String cat = normalize(category);
        if ("equipment".equals(cat)) {
            return true;
        }
        return key.endsWith("_puppet")
                || key.contains("spirit_boat")
                || key.contains("wind_feather_raft")
                || key.contains("cloud_sedan")
                || key.contains("ferry")
                || key.startsWith("alchemy_furnace_g");
    }

    /**
     * Executable bulk talismans: category=talisman (or id ends with _talisman/_fu)
     * excluding paper/materials/recipe sheets and obvious herbs/ores mis-tagged as talisman.
     */
    public static boolean isExecutableTalisman(String id, String category) {
        String key = normalize(id);
        if (key.isBlank()) {
            return false;
        }
        if (isTalismanMaterialOrRecipe(key)) {
            return false;
        }
        String cat = normalize(category);
        if ("talisman".equals(cat)) {
            return true;
        }
        return key.endsWith("_talisman")
                || key.endsWith("_fu")
                || key.contains("talisman_");
    }

    public static boolean isTalismanMaterialOrRecipe(String id) {
        String key = normalize(id);
        if (key.isBlank()) {
            return false;
        }
        if (key.startsWith("recipe_")) {
            return true;
        }
        if (key.contains("talisman_paper") || key.equals("talisman_paper") || key.equals("yin_talisman_paper")) {
            return true;
        }
        // Mis-tagged bulk materials under talisman category.
        return key.equals("ginseng_spirit")
                || key.equals("mirage_sand")
                || key.equals("nether_bone_grass")
                || key.equals("phoenix_tail_fern")
                || key.equals("star_moon_grass")
                || key.equals("thunder_essence_lotus")
                || key.equals("thunder_stone")
                || key.equals("wind_sage_grass");
    }

    /** Bulk manuals that can study via ManualCatalogService even without a manuals_catalog row. */
    public static boolean isCatalogManual(String id, String category) {
        String key = normalize(id);
        if (key.isBlank()) {
            return false;
        }
        if (TextMaterialCatalogService.builtin().findManual(key).isPresent()) {
            return true;
        }
        String cat = normalize(category);
        if ("manual".equals(cat) || "technique".equals(cat)) {
            return true;
        }
        return key.contains("manual")
                || key.endsWith("_art_page")
                || key.endsWith("_art_fragment")
                || key.endsWith("_art_scroll")
                || key.endsWith("_cipher")
                || "talisman_recipe".equals(key)
                || "talisman_recipe_mid".equals(key)
                || "talisman_recipe_high_bundle".equals(key)
                || "array_blueprint_scroll".equals(key)
                || "illusion_scroll".equals(key)
                || "shape_shift_scroll".equals(key)
                || "void_palace_intel_scroll".equals(key)
                || "artifact_identify_scroll".equals(key)
                || "silver_giant_sword_blueprint".equals(key);
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
            if ("sect_contribution_token".equals(key)) {
                return Optional.of(new ConsumableDefinition(
                        key,
                        "Sect Contribution Token",
                        "currency",
                        "",
                        "sect_contribution_redeem",
                        0));
            }
            String synthetic = switch (key) {
                // Bulk carriers without catalog rows still get executable semantics.
                case "mortal_medicine" -> "restore_health";
                case "diyuan_access_token" -> "travel_diyuan";
                case "jade_slip_blank", "paper_formula_scroll" -> "inscribe_formula";
                case "spirit_pill_voucher" -> "redeem_spirit_pill_voucher";
                default -> "";
            };
            if (!synthetic.isBlank()) {
                return Optional.of(new ConsumableDefinition(key, key, "consumable", "", synthetic, 0));
            }
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
            case "sect_contribution_token" -> "sect_contribution_redeem";
            case "spirit_boat_ticket" -> "travel_spirit_boat";
            case "ferry_pass" -> "travel_nether_ferry";
            case "teleport_talisman_chaotic_sea" -> "travel_chaotic_sea";
            case "diyuan_access_token" -> "travel_diyuan";
            case "spirit_gathering_array_disk" -> "deploy_spirit_gather_disk";
            case "auction_invitation" -> "open_auction_invite";
            case "sect_identity_token" -> "show_sect_identity";
            case "star_palace_tax_receipt" -> "star_palace_tax_paid";
            case "star_palace_patrol_seal" -> "star_palace_patrol";
            case "void_palace_map_fragment" -> "discover_void_palace";
            case "fallen_demon_scout_report" -> "discover_fallen_demon";
            case "kunwu_map_scroll" -> "discover_kunwu";
            case "mortal_medicine" -> "restore_health";
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
