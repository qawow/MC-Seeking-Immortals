package com.xunxian.seekingimmortals.cultivation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 可玩种族 + 鬼修路线目录。
 * <p>提供默认种族、路径阶段与合法 id 查询；玩家状态仍存 {@link PlayerCultivation}。</p>
 */
public final class PathRaceCatalog {
    public static final String DEFAULT_RACE_ID = "human_mortal";
    public static final String DEFAULT_PATH_ID = "orthodox";
    public static final String GHOST_PATH_ID = "ghost_cultivator";

    private static final Snapshot BUILTIN = loadBuiltin();

    private PathRaceCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record RaceEntry(String id, String display, String layer, boolean canCultivate, boolean isDefault) {}

    public record GhostStage(String id, String display, String realmEquiv) {}

    public record Snapshot(
            Map<String, RaceEntry> races,
            Map<String, GhostStage> ghostStages,
            String ghostPathId,
            String ghostPathDisplay) {
        public Optional<RaceEntry> findRace(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(races.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public Optional<GhostStage> findGhostStage(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(ghostStages.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public boolean isKnownRace(String id) {
            return findRace(id).isPresent();
        }

        public boolean isGhostPath(String pathId) {
            if (pathId == null || pathId.isBlank()) return false;
            String key = pathId.trim().toLowerCase(Locale.ROOT);
            return key.equals(ghostPathId) || key.equals("ghost") || key.equals("ghost_cultivator");
        }
    }

    public static String sanitizeRaceId(String id) {
        if (id == null || id.isBlank()) return DEFAULT_RACE_ID;
        String key = id.trim().toLowerCase(Locale.ROOT);
        return builtin().findRace(key).map(RaceEntry::id).orElse(key.length() > 64 ? key.substring(0, 64) : key);
    }

    public static String sanitizePathId(String id) {
        if (id == null || id.isBlank()) return DEFAULT_PATH_ID;
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (builtin().isGhostPath(key)) return GHOST_PATH_ID;
        if ("orthodox".equals(key) || "default".equals(key) || "human".equals(key) || "zhengdao".equals(key)) {
            return DEFAULT_PATH_ID;
        }
        return key.length() > 64 ? key.substring(0, 64) : key;
    }

    public static String sanitizeGhostStageId(String id) {
        if (id == null || id.isBlank()) return "";
        String key = id.trim().toLowerCase(Locale.ROOT);
        return builtin().findGhostStage(key).map(GhostStage::id).orElse(key.length() > 64 ? key.substring(0, 64) : key);
    }

    private static Snapshot loadBuiltin() {
        Map<String, RaceEntry> races = new LinkedHashMap<>();
        Map<String, GhostStage> stages = new LinkedHashMap<>();
        String pathId = GHOST_PATH_ID;
        String pathDisplay = "鬼修道途";

        JsonObject raceRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/playable_races.json");
        if (raceRoot == null) {
            raceRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/playable_races_index.json");
        }
        if (raceRoot != null) {
            JsonArray array = raceRoot.has("races") && raceRoot.get("races").isJsonArray()
                    ? raceRoot.getAsJsonArray("races") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                boolean canCultivate = !o.has("can_cultivate") || asBool(o.get("can_cultivate"), true);
                boolean isDefault = o.has("default") && asBool(o.get("default"), false);
                races.put(id, new RaceEntry(id, firstNonBlank(str(o, "display"), id),
                        str(o, "layer"), canCultivate, isDefault));
            }
        }
        if (races.isEmpty()) {
            races.put("human_mortal", new RaceEntry("human_mortal", "凡人", "mortal", true, true));
            races.put("human_cultivator", new RaceEntry("human_cultivator", "人族修士", "cultivator", true, false));
            races.put("demon_cultivator", new RaceEntry("demon_cultivator", "妖修", "cultivator", true, false));
            races.put("ghost_cultivator", new RaceEntry("ghost_cultivator", "鬼修", "cultivator", true, false));
            races.put("mulan_fashi", new RaceEntry("mulan_fashi", "慕兰法士", "cultivator", true, false));
            races.put("spirit_fox_clan", new RaceEntry("spirit_fox_clan", "天狐族", "spirit_realm", true, false));
            races.put("spirit_tree_clan", new RaceEntry("spirit_tree_clan", "树妖族", "spirit_realm", true, false));
            races.put("spirit_stone_clan", new RaceEntry("spirit_stone_clan", "石人族", "spirit_realm", true, false));
            races.put("spirit_fly_clan", new RaceEntry("spirit_fly_clan", "飞灵族", "spirit_realm", true, false));
            races.put("demon_spirit_hybrid", new RaceEntry("demon_spirit_hybrid", "妖化人族", "hybrid", true, false));
        }

        JsonObject ghostRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ghost_cultivation_path.json");
        if (ghostRoot == null) {
            ghostRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/ghost_cultivation_path_index.json");
        }
        if (ghostRoot != null) {
            pathId = firstNonBlank(str(ghostRoot, "path_id"), GHOST_PATH_ID).toLowerCase(Locale.ROOT);
            pathDisplay = firstNonBlank(str(ghostRoot, "display"), pathDisplay);
            JsonArray array = ghostRoot.has("stages") && ghostRoot.get("stages").isJsonArray()
                    ? ghostRoot.getAsJsonArray("stages") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                stages.put(id, new GhostStage(id, firstNonBlank(str(o, "display"), id), str(o, "realm_equiv")));
            }
        }
        if (stages.isEmpty()) {
            stages.put("yin_body", new GhostStage("yin_body", "阴体凝形", "QI_REFINING"));
            stages.put("soul_anchor", new GhostStage("soul_anchor", "魂锚稳固", "FOUNDATION"));
            stages.put("nether_core", new GhostStage("nether_core", "冥核结成", "CORE_FORMATION"));
            stages.put("yin_soul", new GhostStage("yin_soul", "阴婴", "NASCENT_SOUL"));
        }
        return new Snapshot(Collections.unmodifiableMap(races), Collections.unmodifiableMap(stages), pathId, pathDisplay);
    }

    private static boolean asBool(JsonElement element, boolean fallback) {
        try {
            if (element == null || element.isJsonNull()) return fallback;
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
            if (element.isJsonPrimitive()) return Boolean.parseBoolean(element.getAsString());
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = PathRaceCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            return e.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
