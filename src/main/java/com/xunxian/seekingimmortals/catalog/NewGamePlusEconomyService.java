package com.xunxian.seekingimmortals.catalog;

import com.google.gson.JsonArray;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * New-game-plus / difficulty economy fuse: price_mod entry for shops and soft phase consumers.
 * SoftPhaseShellService remains narrative-only; this service owns numeric price coefficients.
 */
public final class NewGamePlusEconomyService {
    private static final String ROOT = "seeking_immortals_ng_economy";
    private static final String DIFFICULTY_KEY = "difficulty";
    private static final Snapshot BUILTIN = loadBuiltin();

    private NewGamePlusEconomyService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static List<DifficultyPreset> presets() {
        return BUILTIN.presets();
    }

    public static Optional<DifficultyPreset> preset(String id) {
        return BUILTIN.find(id);
    }

    public static String difficultyOf(ServerPlayer player) {
        if (player == null) {
            return "standard";
        }
        String id = player.getPersistentData().getCompound(ROOT).getString(DIFFICULTY_KEY);
        return id == null || id.isBlank() ? "standard" : id.trim().toLowerCase(Locale.ROOT);
    }

    public static void setDifficulty(ServerPlayer player, String presetId) {
        if (player == null) {
            return;
        }
        String id = presetId == null || presetId.isBlank() ? "standard" : presetId.trim().toLowerCase(Locale.ROOT);
        if (BUILTIN.find(id).isEmpty()) {
            id = "standard";
        }
        CompoundTag tag = player.getPersistentData().getCompound(ROOT).copy();
        tag.putString(DIFFICULTY_KEY, id);
        player.getPersistentData().put(ROOT, tag);
    }

    public static double priceModFor(ServerPlayer player) {
        return preset(difficultyOf(player)).map(DifficultyPreset::priceMod).orElse(1.0D);
    }

    public static double priceMod(String presetId) {
        return preset(presetId).map(DifficultyPreset::priceMod).orElse(1.0D);
    }

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source.contains(ROOT)) {
            target.put(ROOT, source.getCompound(ROOT).copy());
        }
    }

    public record DifficultyPreset(String id, int liquidYear, double priceMod, double breakthroughBonus,
                                   boolean failHarsher, boolean noNgPlus, String description) {}

    public record Mode(String id, String display, boolean inherit, String description) {}

    public record Snapshot(List<DifficultyPreset> presets, List<Mode> modes, List<String> economyFuseIds) {
        public Optional<DifficultyPreset> find(String id) {
            String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            return presets.stream().filter(p -> p.id().equals(key)).findFirst();
        }
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson(path("text_material/newgame_plus_economy_v102.json"));
        List<DifficultyPreset> presets = new ArrayList<>();
        List<Mode> modes = new ArrayList<>();
        List<String> fuses = new ArrayList<>();
        if (root != null) {
            for (JsonElement element : array(root, "difficulty_presets")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                if (id.isBlank()) {
                    continue;
                }
                presets.add(new DifficultyPreset(
                        id,
                        asInt(o, "liquid_year"),
                        asDouble(o, "price_mod", 1.0D),
                        asDouble(o, "breakthrough_bonus", 0.0D),
                        asBool(o, "fail_harsher"),
                        asBool(o, "no_ng_plus"),
                        str(o, "description")));
            }
            for (JsonElement element : array(root, "modes")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = normalize(str(o, "id"));
                if (id.isBlank()) {
                    continue;
                }
                modes.add(new Mode(id, str(o, "display"), asBool(o, "inherit"), str(o, "description")));
            }
            for (JsonElement element : array(root, "economy_fuses")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                String id = normalize(str(element.getAsJsonObject(), "id"));
                if (!id.isBlank()) {
                    fuses.add(id);
                }
            }
        }
        if (presets.isEmpty()) {
            presets.add(new DifficultyPreset("standard", 6, 1.0D, 0.0D, false, false, "默认"));
        }
        return new Snapshot(List.copyOf(presets), List.copyOf(modes), List.copyOf(fuses));
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = NewGamePlusEconomyService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(o.get(key));
        }
    }

    private static int asInt(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double asDouble(JsonObject o, String key, double fallback) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return o.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean asBool(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonPrimitive()) {
            return false;
        }
        try {
            return o.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
