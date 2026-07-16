package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

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
 * M09: deep runtime catalog for the 19 secret realms.
 * Loads {@code worldpack/secret_realm_runtime.json} (floors / traps / bosses / time / party / gate).
 */
public final class SecretRealmCatalogService {
    private static final Snapshot SNAPSHOT = load();

    private SecretRealmCatalogService() {}

    public record TrapDef(String id, String role, String fieldKind) {}

    public record SpawnDef(String id, int weight) {}

    public record LayerDef(
            String id,
            String display,
            int threatMin,
            int threatMax,
            List<SpawnDef> spawns,
            List<TrapDef> traps,
            List<String> lootHint) {}

    public record RealmDef(
            String id,
            String display,
            String realmMin,
            String cycleId,
            String openCondition,
            String environmentRules,
            String layeredExploration,
            int layersCount,
            List<LayerDef> layers,
            List<String> bosses,
            List<String> lootTiers,
            int partyLimit,
            int timeLimitTicks,
            List<Integer> openWindowDays,
            String regionId,
            String gate,
            String ticketItem,
            int cooldownTicks) {}

    public record Snapshot(Map<String, RealmDef> byId) {
        public int size() {
            return byId.size();
        }

        public Optional<RealmDef> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(byId.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public List<RealmDef> all() {
            return List.copyOf(byId.values());
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<RealmDef> find(String id) {
        return SNAPSHOT.find(id);
    }

    public static List<RealmDef> all() {
        return SNAPSHOT.all();
    }

    public static List<RealmDef> realmsForGate(String gateBlockId) {
        if (gateBlockId == null || gateBlockId.isBlank()) {
            return List.of();
        }
        String key = normalizeGate(gateBlockId);
        List<RealmDef> out = new ArrayList<>();
        for (RealmDef realm : SNAPSHOT.byId().values()) {
            if (key.equals(normalizeGate(realm.gate()))) {
                out.add(realm);
            }
        }
        return List.copyOf(out);
    }

    public static Optional<RealmDef> primaryRealmForGate(String gateBlockId) {
        List<RealmDef> list = realmsForGate(gateBlockId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public static String normalizeGate(String gate) {
        if (gate == null) {
            return "";
        }
        String key = gate.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("seeking_immortals:")) {
            key = key.substring("seeking_immortals:".length());
        }
        if (key.startsWith("gate:")) {
            key = key.substring("gate:".length());
        }
        return key;
    }

    private static Snapshot load() {
        Map<String, RealmDef> byId = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/worldpack/secret_realm_runtime.json");
        if (root == null) {
            SeekingImmortalsMod.LOGGER.warn("M09 secret_realm_runtime.json missing");
            return new Snapshot(Collections.emptyMap());
        }
        for (JsonElement element : array(root, "realms")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = str(object, "id").toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            List<LayerDef> layers = new ArrayList<>();
            for (JsonElement layerEl : array(object, "layers")) {
                if (!layerEl.isJsonObject()) {
                    continue;
                }
                JsonObject layer = layerEl.getAsJsonObject();
                int threatMin = 1;
                int threatMax = 3;
                if (layer.has("threat") && layer.get("threat").isJsonArray()) {
                    JsonArray threat = layer.getAsJsonArray("threat");
                    if (threat.size() >= 1 && threat.get(0).isJsonPrimitive()) {
                        threatMin = Math.max(1, threat.get(0).getAsInt());
                    }
                    if (threat.size() >= 2 && threat.get(1).isJsonPrimitive()) {
                        threatMax = Math.max(threatMin, threat.get(1).getAsInt());
                    }
                }
                List<SpawnDef> spawns = new ArrayList<>();
                for (JsonElement spawnEl : array(layer, "spawns")) {
                    if (spawnEl.isJsonObject()) {
                        JsonObject spawn = spawnEl.getAsJsonObject();
                        String spawnId = str(spawn, "id");
                        if (!spawnId.isBlank()) {
                            spawns.add(new SpawnDef(spawnId, Math.max(1, intValue(spawn, "weight", 1))));
                        }
                    } else if (spawnEl.isJsonPrimitive()) {
                        String spawnId = spawnEl.getAsString();
                        if (!spawnId.isBlank()) {
                            spawns.add(new SpawnDef(spawnId, 1));
                        }
                    }
                }
                List<TrapDef> traps = new ArrayList<>();
                for (JsonElement trapEl : array(layer, "traps")) {
                    if (!trapEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject trap = trapEl.getAsJsonObject();
                    String fieldKind = str(trap, "field_kind");
                    if (fieldKind.isBlank()) {
                        continue;
                    }
                    traps.add(new TrapDef(str(trap, "id"), str(trap, "role"), fieldKind.toUpperCase(Locale.ROOT)));
                }
                layers.add(new LayerDef(
                        str(layer, "id"),
                        str(layer, "display"),
                        threatMin,
                        threatMax,
                        List.copyOf(spawns),
                        List.copyOf(traps),
                        strings(layer, "loot_hint")));
            }
            List<Integer> window = new ArrayList<>();
            for (JsonElement day : array(object, "open_window_days")) {
                if (day.isJsonPrimitive() && day.getAsJsonPrimitive().isNumber()) {
                    window.add(day.getAsInt());
                }
            }
            int layersCount = Math.max(layers.size(), intValue(object, "layers_count", layers.isEmpty() ? 1 : layers.size()));
            RealmDef def = new RealmDef(
                    id,
                    str(object, "display"),
                    str(object, "realm_min"),
                    str(object, "cycle_id"),
                    str(object, "open_condition"),
                    str(object, "environment_rules"),
                    str(object, "layered_exploration"),
                    layersCount,
                    List.copyOf(layers),
                    strings(object, "bosses"),
                    strings(object, "loot_tiers"),
                    Math.max(1, intValue(object, "party_limit", 4)),
                    Math.max(20 * 60, intValue(object, "time_limit_ticks", 20 * 60 * 30)),
                    List.copyOf(window),
                    str(object, "region_id"),
                    str(object, "gate"),
                    str(object, "ticket_item"),
                    Math.max(0, intValue(object, "cooldown_ticks", 72000)));
            byId.put(id, def);
        }
        SeekingImmortalsMod.LOGGER.info("M09 secret realm catalog loaded: {} realms", byId.size());
        return new Snapshot(Collections.unmodifiableMap(byId));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SecretRealmCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key)
                : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(object, key)) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private static String str(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString()
                : "";
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt()
                : fallback;
    }
}
