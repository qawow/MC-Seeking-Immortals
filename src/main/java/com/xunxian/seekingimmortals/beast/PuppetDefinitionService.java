package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;

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
 * M10: puppet definition catalog for SummonedServitor PUPPET variants.
 * Craft recipes remain M04; part items remain M03.
 */
public final class PuppetDefinitionService {
    private static final Snapshot SNAPSHOT = load();

    private PuppetDefinitionService() {}

    public record PuppetDef(
            String id,
            String display,
            String tier,
            double hpBase,
            double damage,
            String role,
            String control,
            String entityIdHint) {

        public SummonedServitorEntity.Archetype archetype() {
            return SummonedServitorEntity.Archetype.PUPPET;
        }

        public int tierIndex() {
            String t = tier == null ? "T0" : tier.trim().toUpperCase(Locale.ROOT);
            if (t.startsWith("T") && t.length() > 1) {
                try {
                    return Integer.parseInt(t.substring(1).replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                    return 0;
                }
            }
            return 0;
        }
    }

    public record Snapshot(Map<String, PuppetDef> byId) {
        public int size() {
            return byId.size();
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<PuppetDef> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.byId().get(id.trim().toLowerCase(Locale.ROOT)));
    }

    public static Map<String, PuppetDef> all() {
        return SNAPSHOT.byId();
    }

    private static Snapshot load() {
        Map<String, PuppetDef> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/puppet_definitions.json");
        if (root != null && root.has("definitions") && root.get("definitions").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("definitions")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) {
                    continue;
                }
                double hp = o.has("hp_base") ? o.get("hp_base").getAsDouble() : 30.0D;
                double dmg = o.has("damage") ? o.get("damage").getAsDouble() : 6.0D;
                String role = "";
                String control = "";
                if (o.has("setting") && o.get("setting").isJsonObject()) {
                    JsonObject s = o.getAsJsonObject("setting");
                    role = str(s, "role");
                    control = str(s, "control");
                }
                map.put(id, new PuppetDef(
                        id,
                        str(o, "display"),
                        str(o, "tier"),
                        hp,
                        dmg,
                        role,
                        control,
                        str(o, "entity_id_hint")));
            }
        }
        return new Snapshot(Collections.unmodifiableMap(map));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = PuppetDefinitionService.class.getClassLoader().getResourceAsStream(path)) {
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
}
