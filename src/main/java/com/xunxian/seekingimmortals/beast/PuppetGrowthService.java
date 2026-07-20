package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent owner-side growth for crafted puppet cores. */
public final class PuppetGrowthService {
    public static final String ROOT = "seeking_immortals_puppet_growth";
    public static final int STAGE_COUNT = 4;
    private static final int ASSEMBLY_EXPERIENCE = 20;
    private static final int HIT_EXPERIENCE = 2;
    private static final int KILL_EXPERIENCE = 12;
    private static final int REPAIR_EXPERIENCE = 45;
    private static final Map<String, String> RECIPE_PUPPETS = loadRecipePuppets();

    public enum CreditKind {
        HIT,
        KILL
    }

    public record GrowthResult(String puppetId, CompanionGrowthService.Progress before,
                               CompanionGrowthService.Update update) {
        public CompanionGrowthService.Progress after() {
            return update.progress();
        }
    }

    private PuppetGrowthService() {}

    public static Map<String, String> recipePuppets() {
        return RECIPE_PUPPETS;
    }

    public static CompanionGrowthService.Progress progress(ServerPlayer player, String summonOrPuppetId) {
        if (player == null) {
            return new CompanionGrowthService.Progress(0, 0, 0);
        }
        String id = puppetIdFromSummonId(summonOrPuppetId);
        CompoundTag entry = player.getPersistentData().getCompound(ROOT).getCompound(id);
        return new CompanionGrowthService.Progress(
                entry.getInt("Level"), entry.getInt("Experience"), entry.getInt("RefinementStage"));
    }

    public static GrowthResult recordAssembly(ServerPlayer player, String summonOrPuppetId) {
        return grant(player, summonOrPuppetId, ASSEMBLY_EXPERIENCE, false);
    }

    public static GrowthResult recordCombatCredit(ServerPlayer player, String summonOrPuppetId, CreditKind kind) {
        int experience = kind == CreditKind.KILL ? KILL_EXPERIENCE : HIT_EXPERIENCE;
        return grant(player, summonOrPuppetId, experience, false);
    }

    public static GrowthResult recordRepair(ServerPlayer player, String summonOrPuppetId,
                                            boolean coreForgeReady) {
        return grant(player, summonOrPuppetId, REPAIR_EXPERIENCE, coreForgeReady);
    }

    public static double statMultiplier(ServerPlayer player, String summonOrPuppetId) {
        return CompanionGrowthService.statMultiplier(progress(player, summonOrPuppetId));
    }

    public static String puppetIdFromSummonId(String summonOrPuppetId) {
        String id = normalize(summonOrPuppetId);
        if (id.startsWith("puppet_")) {
            id = id.substring("puppet_".length());
        }
        String mapped = RECIPE_PUPPETS.get(id);
        if (mapped != null) {
            return mapped;
        }
        if (PuppetDefinitionService.find(id).isPresent()) {
            return id;
        }
        if (!id.endsWith("_puppet") && PuppetDefinitionService.find(id + "_puppet").isPresent()) {
            return id + "_puppet";
        }
        return id;
    }

    public static String describe(ServerPlayer player, String summonOrPuppetId) {
        CompanionGrowthService.Progress progress = progress(player, summonOrPuppetId);
        int needed = progress.level() >= CompanionGrowthService.MAX_LEVEL
                ? 0 : CompanionGrowthService.experienceToNextLevel(progress.level());
        return "level=" + progress.level() + ",experience=" + progress.experience() + "/" + needed
                + ",refinement=" + progress.evolutionStage();
    }

    private static GrowthResult grant(ServerPlayer player, String summonOrPuppetId, int experience,
                                      boolean coreForgeReady) {
        String id = puppetIdFromSummonId(summonOrPuppetId);
        CompanionGrowthService.Progress before = progress(player, id);
        CompanionGrowthService.Update update = CompanionGrowthService.grant(
                before, experience, STAGE_COUNT, coreForgeReady);
        if (player != null && !id.isBlank()) {
            CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
            CompoundTag entry = root.getCompound(id).copy();
            entry.putInt("Level", update.progress().level());
            entry.putInt("Experience", update.progress().experience());
            entry.putInt("RefinementStage", update.progress().evolutionStage());
            root.put(id, entry);
            player.getPersistentData().put(ROOT, root);
        }
        return new GrowthResult(id, before, update);
    }

    private static Map<String, String> loadRecipePuppets() {
        Map<String, String> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID
                + "/text_material/puppet_craft_recipes.json");
        if (root != null && root.has("recipes") && root.get("recipes").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("recipes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String recipe = normalize(str(object, "id"));
                String puppet = normalize(str(object, "puppet_id"));
                if (!recipe.isBlank() && !puppet.isBlank()) {
                    map.put(recipe, puppet);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = PuppetGrowthService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
