package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M07 formation catalog + derived field parameters (radius/kind/aura bonus).
 * Loads formation_field_params.json with fallbacks from formation_catalog / array catalog.
 */
public final class FormationFieldCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private FormationFieldCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record FieldParams(
            String id,
            String display,
            FormationFieldService.FieldKind kind,
            int radius,
            int durationTicks,
            int auraBonus,
            String effect,
            String realmMin,
            boolean usesSpiritGatheringRing
    ) {}

    public record Snapshot(Map<String, FieldParams> fields) {
        public Snapshot {
            fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }

        public int size() {
            return fields.size();
        }

        public Optional<FieldParams> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            FieldParams direct = fields.get(id);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = id.trim().toLowerCase(Locale.ROOT);
            FieldParams lower = fields.get(key);
            if (lower != null) {
                return Optional.of(lower);
            }
            for (FieldParams p : fields.values()) {
                if (p.id().equalsIgnoreCase(id)) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }

        public Optional<FieldParams> findByKind(FormationFieldService.FieldKind kind) {
            if (kind == null) {
                return Optional.empty();
            }
            for (FieldParams p : fields.values()) {
                if (p.kind() == kind) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, FieldParams> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/formation_field_params.json");
        if (root != null) {
            for (JsonElement element : array(root, "fields")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                FormationFieldService.FieldKind kind = parseKind(str(o, "kind"), id, str(o, "effect"));
                int radius = Math.max(1, intVal(o, "radius", kind.radius()));
                boolean ring = o.has("uses_spirit_gathering_ring")
                        ? bool(o, "uses_spirit_gathering_ring", kind.usesSpiritGatheringRing())
                        : kind.usesSpiritGatheringRing();
                map.put(id, new FieldParams(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        kind,
                        radius,
                        Math.max(20, intVal(o, "duration_ticks", 20 * 90)),
                        Math.max(0, intVal(o, "aura_bonus", kind == FormationFieldService.FieldKind.SPIRIT_GATHER ? 50 : 0)),
                        str(o, "effect"),
                        str(o, "realm_min"),
                        ring
                ));
            }
        }

        // Merge formation_catalog ids that may not be in params file.
        JsonObject catalog = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/formation_catalog.json");
        if (catalog != null) {
            for (JsonElement element : array(catalog, "formations")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank() || map.containsKey(id)) {
                    continue;
                }
                FormationFieldService.FieldKind kind = parseKind("", id, str(o, "effect").isBlank() ? str(o, "use") : str(o, "effect"));
                map.put(id, new FieldParams(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        kind,
                        kind.radius(),
                        20 * 90,
                        kind == FormationFieldService.FieldKind.SPIRIT_GATHER ? 50 : 0,
                        str(o, "effect"),
                        str(o, "realm_min"),
                        kind.usesSpiritGatheringRing()
                ));
            }
        }

        // Array catalog expansion.
        JsonObject arrays = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/formation_array_catalog_v97.json");
        if (arrays != null) {
            for (JsonElement element : array(arrays, "arrays")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank() || map.containsKey(id)) {
                    continue;
                }
                int grade = intVal(o, "grade", 2);
                FormationFieldService.FieldKind kind = parseKind("", id, str(o, "type") + " " + str(o, "effect"));
                int radius = Math.min(8, Math.max(1, grade));
                map.put(id, new FieldParams(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        kind,
                        radius,
                        20 * 90,
                        kind == FormationFieldService.FieldKind.SPIRIT_GATHER ? 40 : 0,
                        str(o, "effect"),
                        "",
                        kind.usesSpiritGatheringRing()
                ));
            }
        }
        return new Snapshot(map);
    }

    static FormationFieldService.FieldKind parseKind(String kindToken, String id, String effect) {
        String k = kindToken == null ? "" : kindToken.trim().toUpperCase(Locale.ROOT);
        if (!k.isBlank()) {
            try {
                return FormationFieldService.FieldKind.valueOf(k);
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        String hay = ((id == null ? "" : id) + " " + (effect == null ? "" : effect)).toLowerCase(Locale.ROOT);
        if (hay.contains("spirit") || hay.contains("gather") || hay.contains("juling") || hay.contains("聚灵")
                || hay.contains("cultivation") || hay.contains("辅助")) {
            return FormationFieldService.FieldKind.SPIRIT_GATHER;
        }
        if (hay.contains("defense") || hay.contains("barrier") || hay.contains("wall") || hay.contains("护")
                || hay.contains("罩") || hay.contains("防护")) {
            return FormationFieldService.FieldKind.DEFENSE;
        }
        if (hay.contains("kill") || hay.contains("sword") || hay.contains("攻") || hay.contains("杀") || hay.contains("剑")) {
            return FormationFieldService.FieldKind.KILL_SWORD;
        }
        if (hay.contains("seal") || hay.contains("demon") || hay.contains("prison") || hay.contains("封") || hay.contains("禁")) {
            return FormationFieldService.FieldKind.SEAL_DEMON;
        }
        if (hay.contains("illusion") || hay.contains("maze") || hay.contains("幻") || hay.contains("迷")) {
            return FormationFieldService.FieldKind.ILLUSION_MAZE;
        }
        return FormationFieldService.FieldKind.CATALOG_GENERIC;
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = FormationFieldCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.warn("Failed loading formation field catalog {}", path, e);
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
            return o.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intVal(JsonObject o, String key, int def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return def;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return def;
        }
    }
}
