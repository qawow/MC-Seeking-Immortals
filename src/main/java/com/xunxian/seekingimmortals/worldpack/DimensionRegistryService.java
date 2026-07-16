package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
 * M13 authoritative dimension-id registry.
 * <p>Loads dimensions_catalog + dimension_registry + datapack reconcile.
 * Exposes id / cosmology / realm band / entry conditions for M06/M09/M10 consumers.
 * SpiritualAuraManager known dims (tianyuan / spirit_fengyuan / yin_ming_pocket /
 * nether_river_pocket / demon_rift) are always present.</p>
 */
public final class DimensionRegistryService {
    public static final String MORTAL_WORLD = "seeking_immortals:mortal_world";
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String TIANYUAN = "seeking_immortals:tianyuan";
    public static final String SPIRIT_FENGYUAN = "seeking_immortals:spirit_fengyuan";
    public static final String YIN_MING_POCKET = "seeking_immortals:yin_ming_pocket";
    public static final String NETHER_RIVER_POCKET = "seeking_immortals:nether_river_pocket";
    public static final String DEMON_RIFT = "seeking_immortals:demon_rift";
    public static final String IMMORTAL_REALM = "seeking_immortals:immortal_realm";
    public static final String ASURA_REALM = "seeking_immortals:asura_realm";

    private static final Snapshot SNAPSHOT = load();

    private DimensionRegistryService() {}

    public record DimensionDef(
            String id,
            String display,
            String cosmology,
            String minRealm,
            String realmCap,
            String minecraftLayer,
            String status,
            String note,
            List<String> mapsTo,
            boolean playable) {
        public boolean isDeferred() {
            return status != null && status.startsWith("deferred");
        }

        public String effectiveMinecraftId() {
            if (MORTAL_WORLD.equals(id) || "overworld".equalsIgnoreCase(minecraftLayer)) {
                return OVERWORLD;
            }
            if (mapsTo != null && !mapsTo.isEmpty()) {
                return mapsTo.get(0);
            }
            return id;
        }
    }

    public record Snapshot(Map<String, DimensionDef> byId, List<String> deferredIds) {
        public int size() {
            return byId.size();
        }

        public Optional<DimensionDef> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            String key = normalize(id);
            DimensionDef direct = byId.get(key);
            if (direct != null) {
                return Optional.of(direct);
            }
            // bare path
            if (!key.contains(":")) {
                DimensionDef namespaced = byId.get(SeekingImmortalsMod.MODID + ":" + key);
                if (namespaced != null) {
                    return Optional.of(namespaced);
                }
            }
            if (OVERWORLD.equals(key) || "overworld".equals(key)) {
                return Optional.ofNullable(byId.get(MORTAL_WORLD));
            }
            return Optional.empty();
        }

        public List<DimensionDef> all() {
            return List.copyOf(byId.values());
        }

        public List<DimensionDef> playable() {
            List<DimensionDef> out = new ArrayList<>();
            for (DimensionDef def : byId.values()) {
                if (def.playable() && !def.isDeferred()) {
                    out.add(def);
                }
            }
            return List.copyOf(out);
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<DimensionDef> find(String id) {
        return SNAPSHOT.find(id);
    }

    public static List<DimensionDef> all() {
        return SNAPSHOT.all();
    }

    public static List<String> deferredIds() {
        return SNAPSHOT.deferredIds();
    }

    public static boolean isKnown(String id) {
        return SNAPSHOT.find(id).isPresent();
    }

    public static String cosmologyOf(String id) {
        return find(id).map(DimensionDef::cosmology).orElse("");
    }

    public static String minRealmOf(String id) {
        return find(id).map(DimensionDef::minRealm).orElse("");
    }

    /** Normalize author/registry ids to the MC dimension ResourceLocation string used at runtime. */
    public static String toMinecraftDimensionId(String id) {
        Optional<DimensionDef> def = find(id);
        if (def.isPresent()) {
            return def.get().effectiveMinecraftId();
        }
        String key = normalize(id);
        if (key.isBlank() || MORTAL_WORLD.equals(key) || "mortal_world".equals(key) || "overworld".equals(key)) {
            return OVERWORLD;
        }
        if (!key.contains(":")) {
            return SeekingImmortalsMod.MODID + ":" + key;
        }
        return key;
    }

    public static ResourceKey<Level> resourceKey(String id) {
        ResourceLocation location = ResourceLocation.tryParse(toMinecraftDimensionId(id));
        if (location == null) {
            return Level.OVERWORLD;
        }
        return ResourceKey.create(Registries.DIMENSION, location);
    }

    public static Optional<ServerLevel> resolveLevel(ServerPlayer player, String id) {
        if (player == null || player.server == null) {
            return Optional.empty();
        }
        ServerLevel level = player.server.getLevel(resourceKey(id));
        return Optional.ofNullable(level);
    }

    public static boolean meetsEntryRealm(ServerPlayer player, String dimensionId) {
        String min = minRealmOf(dimensionId);
        if (min == null || min.isBlank()) {
            return true;
        }
        return ProgressionGateApi.meetsRealm(player, min);
    }

    public static boolean meetsEntryRealm(Realm playerRealm, String dimensionId) {
        String min = minRealmOf(dimensionId);
        if (min == null || min.isBlank()) {
            return true;
        }
        Realm required = Realm.fromDesignId(min);
        if (required == null || playerRealm == null) {
            return true;
        }
        return playerRealm.ordinal() >= required.ordinal();
    }

    /** Required aura-known dims must always be registered for M06/M10 consumers. */
    public static boolean coversAuraKnownDimensions() {
        return isKnown(TIANYUAN)
                && isKnown(SPIRIT_FENGYUAN)
                && isKnown(YIN_MING_POCKET)
                && isKnown(NETHER_RIVER_POCKET)
                && isKnown(DEMON_RIFT);
    }

    private static Snapshot load() {
        Map<String, DimensionDef> map = new LinkedHashMap<>();
        List<String> deferred = new ArrayList<>();

        // Prefer full catalog, then registry index, then hard seeds.
        ingestCatalog(map, deferred, readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/dimensions_catalog.json"));
        ingestRegistry(map, deferred, readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/dimension_registry.json"));
        ingestIndex(map, deferred, readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/dimension_registry_index.json"));
        ingestIndex(map, deferred, readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/dimensions_index.json"));
        seedRequired(map, deferred);

        return new Snapshot(Collections.unmodifiableMap(map), List.copyOf(deferred));
    }

    private static void seedRequired(Map<String, DimensionDef> map, List<String> deferred) {
        putIfAbsent(map, def(MORTAL_WORLD, "人界", "mortal_realm", "", "DEITY_TRANSFORMATION", "overworld", "", "", List.of(OVERWORLD), true));
        putIfAbsent(map, def(TIANYUAN, "天渊城", "spirit_realm", "DEITY_TRANSFORMATION", "GREAT_VEHICLE", "custom", "", "", List.of(), true));
        putIfAbsent(map, def(SPIRIT_FENGYUAN, "灵界·风元大陆", "spirit_realm", "VOID_REFINEMENT", "GREAT_VEHICLE", "custom", "", "", List.of(), true));
        putIfAbsent(map, def(YIN_MING_POCKET, "阴冥口袋", "yin_underworld_cluster", "FOUNDATION", "", "pocket", "", "", List.of(), true));
        putIfAbsent(map, def(NETHER_RIVER_POCKET, "冥河口袋", "yin_underworld_cluster", "FOUNDATION", "", "pocket", "", "", List.of(), true));
        putIfAbsent(map, def(DEMON_RIFT, "魔界裂隙", "ancient_demon_realm", "NASCENT_SOUL", "", "event", "", "", List.of(), true));
        putIfAbsent(map, def(IMMORTAL_REALM, "仙界", "immortal_realm", "GREAT_VEHICLE", "", "reserved", "", "", List.of(), true));
        putIfAbsent(map, def(ASURA_REALM, "修罗界", "secret_realm_instance", "", "", "custom", "", "", List.of(), true));
        putIfAbsent(map, def("seeking_immortals:secret_realm_instance", "秘境实例模板", "secret_realm_instance",
                "", "", "instanced", "deferred_template", "模板类型，非独立可进入维度", List.of(), false));
        putIfAbsent(map, def("seeking_immortals:yin_underworld", "阴司集群（逻辑）", "yin_underworld_cluster",
                "", "", "pocket", "deferred_logical", "逻辑集群；实际口袋 yin_ming_pocket / nether_river_pocket",
                List.of(YIN_MING_POCKET, NETHER_RIVER_POCKET), false));
        for (DimensionDef def : map.values()) {
            if (def.isDeferred() && !deferred.contains(def.id())) {
                deferred.add(def.id());
            }
        }
    }

    private static void ingestCatalog(Map<String, DimensionDef> map, List<String> deferred, JsonObject root) {
        if (root == null) {
            return;
        }
        for (JsonElement element : array(root, "dimensions")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = firstNonBlank(str(o, "dimension_id"), str(o, "id"), str(o, "registry_id"));
            if (id.isBlank()) {
                continue;
            }
            List<String> mapsTo = new ArrayList<>();
            if (id.endsWith("mortal_world")) {
                mapsTo.add(OVERWORLD);
            }
            DimensionDef def = def(
                    normalize(id),
                    firstNonBlank(str(o, "display"), id),
                    firstNonBlank(str(o, "cosmology"), str(o, "type")),
                    firstNonBlank(str(o, "realm_min_entry"), str(o, "min_realm")),
                    str(o, "realm_cap"),
                    str(o, "minecraft_layer"),
                    str(o, "status"),
                    str(o, "note"),
                    mapsTo,
                    !"false".equalsIgnoreCase(String.valueOf(o.has("playable") ? o.get("playable") : true)));
            putIfAbsent(map, def);
        }
    }

    private static void ingestRegistry(Map<String, DimensionDef> map, List<String> deferred, JsonObject root) {
        if (root == null) {
            return;
        }
        for (JsonElement element : array(root, "dimensions")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = firstNonBlank(str(o, "registry_id"), str(o, "id"));
            if (id.isBlank()) {
                continue;
            }
            String layer = "";
            if (o.has("minecraft") && o.get("minecraft").isJsonObject()) {
                layer = str(o.getAsJsonObject("minecraft"), "type");
            }
            List<String> mapsTo = new ArrayList<>();
            if ("overworld".equalsIgnoreCase(layer) || id.endsWith("mortal_world")) {
                mapsTo.add(OVERWORLD);
            }
            String status = "";
            String note = "";
            if (id.endsWith("yin_underworld")) {
                status = "deferred_logical";
                note = "逻辑集群";
                mapsTo = List.of(YIN_MING_POCKET, NETHER_RIVER_POCKET);
                deferred.add(normalize(id));
            }
            if (id.endsWith("secret_realm_instance")) {
                status = "deferred_template";
                note = "模板类型";
                deferred.add(normalize(id));
            }
            putIfAbsent(map, def(
                    normalize(id),
                    firstNonBlank(str(o, "display"), id),
                    firstNonBlank(str(o, "cosmology"), str(o, "region")),
                    str(o, "entry_realm_min"),
                    "",
                    layer,
                    status,
                    note,
                    mapsTo,
                    status.isBlank()));
        }
    }

    private static void ingestIndex(Map<String, DimensionDef> map, List<String> deferred, JsonObject root) {
        if (root == null) {
            return;
        }
        for (JsonElement element : array(root, "dimensions")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = firstNonBlank(str(o, "id"), str(o, "key"), str(o, "registry_id"));
            if (id.isBlank() || "cosmology".equals(str(o, "type"))) {
                // cosmology-only rows stay optional metadata
                if ("cosmology".equals(str(o, "type"))) {
                    continue;
                }
            }
            if (id.isBlank()) {
                continue;
            }
            List<String> mapsTo = new ArrayList<>();
            if (o.has("maps_to") && o.get("maps_to").isJsonArray()) {
                for (JsonElement m : o.getAsJsonArray("maps_to")) {
                    try {
                        mapsTo.add(m.getAsString());
                    } catch (Exception ignored) {
                    }
                }
            }
            String status = str(o, "status");
            if (!status.isBlank()) {
                deferred.add(normalize(id));
            }
            putIfAbsent(map, def(
                    normalize(id),
                    firstNonBlank(str(o, "display"), id),
                    firstNonBlank(str(o, "cosmology"), str(o, "type")),
                    firstNonBlank(str(o, "min_realm"), str(o, "entry_realm_min")),
                    str(o, "realm_cap"),
                    str(o, "minecraft_layer"),
                    status,
                    str(o, "note"),
                    mapsTo,
                    status.isBlank()));
        }
    }

    private static void putIfAbsent(Map<String, DimensionDef> map, DimensionDef def) {
        if (def == null || def.id().isBlank()) {
            return;
        }
        map.putIfAbsent(def.id(), def);
    }

    private static DimensionDef def(String id, String display, String cosmology, String minRealm, String realmCap,
                                    String layer, String status, String note, List<String> mapsTo, boolean playable) {
        return new DimensionDef(
                normalize(id),
                display == null ? id : display,
                cosmology == null ? "" : cosmology,
                minRealm == null ? "" : minRealm,
                realmCap == null ? "" : realmCap,
                layer == null ? "" : layer,
                status == null ? "" : status,
                note == null ? "" : note,
                mapsTo == null ? List.of() : List.copyOf(mapsTo),
                playable);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DimensionRegistryService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            JsonElement element = object.get(key);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
            return element.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String id) {
        if (id == null) {
            return "";
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:overworld") || key.equals("overworld")) {
            return MORTAL_WORLD;
        }
        return key;
    }
}
