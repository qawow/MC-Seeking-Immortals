package com.xunxian.seekingimmortals.alchemy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AlchemyRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PACKAGED_MANIFEST =
            "data/seeking_immortals/alchemy/recipe_manifest.json";
    private static final String PACKAGED_RECIPE_ROOT =
            "data/seeking_immortals/alchemy/recipes/";
    private static volatile List<AlchemyRecipe> recipes = AlchemyRecipe.builtinRecipes();
    private static volatile Map<String, AlchemyRecipe> recipesById = byId(AlchemyRecipe.builtinRecipes());

    public AlchemyRecipeManager() {
        super(GSON, "alchemy/recipes");
    }

    public static Collection<AlchemyRecipe> recipes() {
        return recipes;
    }

    /**
     * Full client-visible corpus for recipe viewers. Packaged datapack recipes
     * provide the baseline on remote clients, then any locally reloaded runtime
     * entries override or extend them by id.
     */
    public static List<AlchemyRecipe> jeiRecipes() {
        Map<String, AlchemyRecipe> merged = new LinkedHashMap<>();
        for (AlchemyRecipe recipe : PackagedHolder.RECIPES) {
            merged.put(recipe.id(), recipe);
        }
        for (AlchemyRecipe recipe : recipes) {
            merged.put(recipe.id(), recipe);
        }
        return List.copyOf(merged.values());
    }

    public static Optional<AlchemyRecipe> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String raw = id.trim();
        AlchemyRecipe recipe = recipesById.get(AlchemyFormulaKnowledge.canonicalRecipeId(raw));
        if (recipe == null) {
            // Preserve compatibility with datapacks that intentionally use a
            // case-sensitive or namespaced custom id outside the legacy map.
            recipe = recipesById.get(raw);
        }
        return Optional.ofNullable(recipe);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<AlchemyRecipe> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
            try {
                loaded.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "alchemy recipe")));
            } catch (RuntimeException ex) {
                SeekingImmortalsMod.LOGGER.warn("Skipping invalid alchemy recipe {}: {}", entry.getKey(), ex.getMessage());
            }
        }
        if (loaded.isEmpty()) {
            installRecipes(AlchemyRecipe.builtinRecipes());
            SeekingImmortalsMod.LOGGER.warn("No valid datapack alchemy recipes loaded; using {} built-in recipes.", recipes.size());
            return;
        }
        installRecipes(loaded);
        SeekingImmortalsMod.LOGGER.info("Loaded {} datapack alchemy recipes.", recipes.size());
    }

    private static void installRecipes(List<AlchemyRecipe> loaded) {
        recipes = List.copyOf(loaded);
        recipesById = byId(loaded);
    }

    private static List<AlchemyRecipe> loadPackagedRecipes() {
        ClassLoader loader = AlchemyRecipeManager.class.getClassLoader();
        List<AlchemyRecipe> loaded = new ArrayList<>();
        try (InputStream stream = loader.getResourceAsStream(PACKAGED_MANIFEST)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing packaged alchemy recipe manifest {}", PACKAGED_MANIFEST);
                return AlchemyRecipe.builtinRecipes();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                for (JsonElement element : GsonHelper.getAsJsonArray(root, "recipes")) {
                    String id = GsonHelper.convertToString(element, "packaged alchemy recipe id");
                    String path = PACKAGED_RECIPE_ROOT + id + ".json";
                    try (InputStream recipeStream = loader.getResourceAsStream(path)) {
                        if (recipeStream == null) {
                            SeekingImmortalsMod.LOGGER.warn("Missing packaged alchemy recipe {}", path);
                            continue;
                        }
                        try (Reader recipeReader = new InputStreamReader(recipeStream, StandardCharsets.UTF_8)) {
                            JsonObject object = GSON.fromJson(recipeReader, JsonObject.class);
                            loaded.add(parse(new ResourceLocation(SeekingImmortalsMod.MODID, id), object));
                        }
                    } catch (RuntimeException exception) {
                        SeekingImmortalsMod.LOGGER.warn(
                                "Skipping invalid packaged alchemy recipe {}: {}", id, exception.getMessage());
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            SeekingImmortalsMod.LOGGER.warn(
                    "Unable to load packaged alchemy recipes: {}", exception.getMessage());
        }
        if (loaded.isEmpty()) {
            return AlchemyRecipe.builtinRecipes();
        }
        return List.copyOf(loaded);
    }

    private static Map<String, AlchemyRecipe> byId(List<AlchemyRecipe> loaded) {
        Map<String, AlchemyRecipe> map = new LinkedHashMap<>();
        for (AlchemyRecipe recipe : loaded) {
            map.put(recipe.id(), recipe);
        }
        return Map.copyOf(map);
    }

    private static AlchemyRecipe parse(ResourceLocation fileId, JsonObject object) {
        String id = GsonHelper.getAsString(object, "id", fileId.getPath());
        return new AlchemyRecipe(
                id,
                displayText(object, id),
                parseOutputs(object),
                positive(GsonHelper.getAsInt(object, "output_count", 1), "output_count"),
                nonNegative(GsonHelper.getAsInt(object, "mana_cost"), "mana_cost"),
                positive(GsonHelper.getAsInt(object, "cook_ticks"), "cook_ticks"),
                clamp01(GsonHelper.getAsDouble(object, "success_rate")),
                clamp01(GsonHelper.getAsDouble(object, "explosion_chance")),
                positive(GsonHelper.getAsInt(object, "required_furnace_tier"), "required_furnace_tier"),
                positive(GsonHelper.getAsInt(object, "ideal_fire_tier"), "ideal_fire_tier"),
                parseRealm(GsonHelper.getAsString(object, "min_control_realm", "MORTAL")),
                GsonHelper.getAsBoolean(object, "controlled", false),
                GsonHelper.getAsBoolean(object, "requires_earth_fire_room", false),
                parseIngredients(object));
    }

    private static net.minecraft.network.chat.Component displayText(JsonObject object, String id) {
        String authored = GsonHelper.getAsString(object, "display_translation", "").trim();
        if (!authored.isBlank()) {
            if (PlayerDisplayText.hasTranslation(authored)) {
                return net.minecraft.network.chat.Component.translatable(authored);
            }
            // A literal is accepted only when it is clearly player-facing Chinese text.
            if (PlayerDisplayText.isSafe(authored)) {
                return net.minecraft.network.chat.Component.literal(authored);
            }
        }
        return AlchemyDisplayTexts.recipe(id);
    }

    private static List<Item> parseOutputs(JsonObject object) {
        JsonObject outputs = object.has("output_items")
                ? GsonHelper.getAsJsonObject(object, "output_items")
                : GsonHelper.getAsJsonObject(object, "outputs");
        // Accept both design names (middle/perfect) and legacy (medium/supreme).
        return List.of(
                parseItem(firstString(outputs, "low", "inferior")),
                parseItem(firstString(outputs, "middle", "medium", "standard")),
                parseItem(firstString(outputs, "high", "superior")),
                parseItem(firstString(outputs, "perfect", "supreme", "peerless")));
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                return GsonHelper.getAsString(object, key);
            }
        }
        throw new IllegalArgumentException("missing output quality key among " + String.join("/", keys));
    }

    private static List<AlchemyRecipe.IngredientRequirement> parseIngredients(JsonObject object) {
        List<AlchemyRecipe.IngredientRequirement> ingredients = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(object, "ingredients")) {
            JsonObject ingredient = GsonHelper.convertToJsonObject(element, "ingredient");
            ingredients.add(new AlchemyRecipe.IngredientRequirement(
                    parseItem(GsonHelper.getAsString(ingredient, "item")),
                    positive(GsonHelper.getAsInt(ingredient, "count", 1), "ingredient.count")));
        }
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("ingredients must not be empty");
        }
        return List.copyOf(ingredients);
    }

    private static Item parseItem(String rawId) {
        String namespaced = rawId.indexOf(':') >= 0 ? rawId : SeekingImmortalsMod.MODID + ":" + rawId;
        ResourceLocation id = ResourceLocation.tryParse(namespaced);
        if (id == null) {
            throw new IllegalArgumentException("invalid item id " + rawId);
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            throw new IllegalArgumentException("unknown item id " + id);
        }
        return item;
    }

    private static Realm parseRealm(String raw) {
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "FOUNDATION" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "CORE", "CORE_FORMATION" -> Realm.CORE_FORMATION;
            case "NASCENT", "NASCENT_SOUL" -> Realm.NASCENT_SOUL;
            case "SPIRIT_SEVERANCE", "SOUL_TRANSFORMATION" -> Realm.SOUL_TRANSFORMATION;
            default -> Realm.valueOf(raw.toUpperCase(Locale.ROOT));
        };
    }

    private static int positive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static int nonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static final class PackagedHolder {
        private static final List<AlchemyRecipe> RECIPES = loadPackagedRecipes();
    }
}
