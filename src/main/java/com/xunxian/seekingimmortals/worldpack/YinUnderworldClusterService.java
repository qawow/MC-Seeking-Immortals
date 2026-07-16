package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
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
 * M13 yin_underworld_cluster passage rules linked to ghost-path (M01).
 */
public final class YinUnderworldClusterService {
    private static final Snapshot SNAPSHOT = load();

    private YinUnderworldClusterService() {}

    public record RegionDef(String id, String display, String realmMin, String path, String secretRealm) {}

    public record PocketDef(String id, String parent, String realmMin) {}

    public record Snapshot(
            String cosmologyId,
            String display,
            Map<String, RegionDef> regions,
            Map<String, PocketDef> pockets,
            List<String> pathAny,
            List<String> questChains) {
        public int regionCount() { return regions.size(); }
        public int pocketCount() { return pockets.size(); }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static boolean isYinDimension(String dimensionId) {
        String key = DimensionRegistryService.toMinecraftDimensionId(dimensionId);
        if (key.endsWith("yin_ming_pocket") || key.endsWith("nether_river_pocket")) {
            return true;
        }
        if (key.endsWith("yin_underworld")) {
            return true;
        }
        return SNAPSHOT.pockets.containsKey(key) || SNAPSHOT.pockets.containsKey(dimensionId == null ? "" : dimensionId);
    }

    public static boolean isYinRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return false;
        }
        return SNAPSHOT.regions.containsKey(regionId.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean canEnter(ServerPlayer player, String dimensionOrRegion) {
        if (player == null) {
            return false;
        }
        String key = dimensionOrRegion == null ? "" : dimensionOrRegion.trim().toLowerCase(Locale.ROOT);
        String realmMin = "FOUNDATION";
        String path = "";
        Optional<RegionDef> region = Optional.ofNullable(SNAPSHOT.regions.get(key));
        if (region.isPresent()) {
            realmMin = firstNonBlank(region.get().realmMin(), realmMin);
            path = region.get().path();
        } else {
            String dim = DimensionRegistryService.toMinecraftDimensionId(key);
            PocketDef pocket = SNAPSHOT.pockets.get(dim);
            if (pocket == null) {
                pocket = SNAPSHOT.pockets.get(key);
            }
            if (pocket != null) {
                realmMin = firstNonBlank(pocket.realmMin(), realmMin);
                RegionDef parent = SNAPSHOT.regions.get(pocket.parent());
                if (parent != null) {
                    path = parent.path();
                }
            }
        }
        if (!ProgressionGateApi.meetsRealm(player, realmMin)) {
            return false;
        }
        // ghost path or quest flag opens full access; otherwise allow with realm only for nether ferry
        boolean ghost = CultivationHelper.get(player).map(c -> c.isGhostPath()).orElse(false)
                || ProgressionGateApi.meetsPath(player, "ghost")
                || ProgressionGateApi.meetsPath(player, "ghost_cultivator");
        boolean quest = NpcDialogueFlags.hasFlag(player, "ghost_path")
                || NpcDialogueFlags.hasFlag(player, "quest_soft_ghost_path")
                || NpcDialogueFlags.hasFlag(player, "yin_luo_initiation");
        if (path != null && path.contains("ghost") && !(ghost || quest)) {
            // soft gate: still allow if player holds yin_stone-like flag or creative
            if (!player.getAbilities().instabuild && !NpcDialogueFlags.hasFlag(player, "yin_ferry_pass")) {
                // nether_river allows foundation+ without ghost; yinming prefers ghost
                if (key.contains("yinming") || key.contains("yin_ming")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<String> passageNotes(ServerPlayer player) {
        List<String> notes = new ArrayList<>();
        notes.add("cosmology=" + SNAPSHOT.cosmologyId);
        notes.add("regions=" + SNAPSHOT.regionCount());
        notes.add("pockets=" + SNAPSHOT.pocketCount());
        if (player != null) {
            boolean ghost = CultivationHelper.get(player).map(c -> c.isGhostPath()).orElse(false);
            notes.add("ghost_path=" + ghost);
        }
        return notes;
    }

    private static Snapshot load() {
        Map<String, RegionDef> regions = new LinkedHashMap<>();
        Map<String, PocketDef> pockets = new LinkedHashMap<>();
        List<String> pathAny = new ArrayList<>();
        List<String> quests = new ArrayList<>();
        String cosmology = "yin_underworld_cluster";
        String display = "阴司之界";

        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/yin_underworld_cluster.json");
        if (root != null) {
            cosmology = firstNonBlank(str(root, "cosmology_id"), cosmology);
            display = firstNonBlank(str(root, "display"), display);
            for (JsonElement element : array(root, "regions")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                regions.put(id.toLowerCase(Locale.ROOT), new RegionDef(
                        id, firstNonBlank(str(o, "display"), id), str(o, "realm_min"),
                        firstNonBlank(str(o, "path"), pathFromLearn(o)), str(o, "secret_realm")));
            }
            for (JsonElement element : array(root, "pocket_dimensions")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                pockets.put(id.toLowerCase(Locale.ROOT), new PocketDef(id, str(o, "parent"), str(o, "realm_min")));
            }
            if (root.has("learn_requirements") && root.get("learn_requirements").isJsonObject()) {
                JsonObject lr = root.getAsJsonObject("learn_requirements");
                if (lr.has("enter") && lr.get("enter").isJsonObject()) {
                    JsonObject enter = lr.getAsJsonObject("enter");
                    for (JsonElement e : array(enter, "path_any")) {
                        try { pathAny.add(e.getAsString()); } catch (Exception ignored) {}
                    }
                }
            }
            for (JsonElement e : array(root, "quest_chains")) {
                try { quests.add(e.getAsString()); } catch (Exception ignored) {}
            }
        }
        if (regions.isEmpty()) {
            regions.put("yinming", new RegionDef("yinming", "阴冥之地", "QI_REFINING", "ghost_cultivator_entry", "yinming_pocket"));
            regions.put("nether_river", new RegionDef("nether_river", "冥河", "FOUNDATION", "", "nether_river_land"));
        }
        if (pockets.isEmpty()) {
            pockets.put(DimensionRegistryService.NETHER_RIVER_POCKET,
                    new PocketDef(DimensionRegistryService.NETHER_RIVER_POCKET, "nether_river", "FOUNDATION"));
            pockets.put(DimensionRegistryService.YIN_MING_POCKET,
                    new PocketDef(DimensionRegistryService.YIN_MING_POCKET, "yinming", "FOUNDATION"));
        }
        if (pathAny.isEmpty()) {
            pathAny = List.of("ghost_cultivator", "yin_luo_quest");
        }
        return new Snapshot(cosmology, display, Collections.unmodifiableMap(regions),
                Collections.unmodifiableMap(pockets), List.copyOf(pathAny), List.copyOf(quests));
    }

    private static String pathFromLearn(JsonObject region) {
        if (region.has("learn_requirements") && region.get("learn_requirements").isJsonObject()) {
            JsonObject lr = region.getAsJsonObject("learn_requirements");
            if (lr.has("enter") && lr.get("enter").isJsonObject()) {
                return str(lr.getAsJsonObject("enter"), "path");
            }
        }
        return "";
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = YinUnderworldClusterService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return new JsonArray();
        return object.getAsJsonArray(key);
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
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }
}
