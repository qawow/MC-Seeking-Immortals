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
 * 渡劫规则目录。波数/基准伤害对齐 {@code tribulation_rules.json}。
 */
public final class TribulationRulesCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private TribulationRulesCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Rule(
            String id,
            String display,
            String triggerRealmId,
            int waves,
            double damagePerWaveBase,
            String tribulationTier) {}

    public record Snapshot(Map<String, Rule> byId, Map<Realm, Rule> byRealm) {
        public Optional<Rule> find(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            return Optional.ofNullable(byId.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public Optional<Rule> forRealm(Realm realm) {
            if (realm == null) return Optional.empty();
            Rule direct = byRealm.get(realm);
            if (direct != null) return Optional.of(direct);
            // 结丹劫语料未单列：沿用小劫参数
            if (realm == Realm.CORE_FORMATION) {
                return Optional.ofNullable(byId.get("minor_soul_trial"));
            }
            return Optional.empty();
        }

        public int strikeCount(Realm realm) {
            return forRealm(realm).map(Rule::waves).orElseGet(() -> fallbackStrikeCount(realm));
        }

        public double damagePerWaveBase(Realm realm) {
            return forRealm(realm).map(Rule::damagePerWaveBase).orElse(0.0D);
        }
    }

    public static int strikeCount(Realm realm) {
        return builtin().strikeCount(realm);
    }

    public static double damagePerWaveBase(Realm realm) {
        return builtin().damagePerWaveBase(realm);
    }

    private static int fallbackStrikeCount(Realm realm) {
        if (realm == null) return 0;
        return switch (realm) {
            case CORE_FORMATION, NASCENT_SOUL -> 3;
            case SOUL_TRANSFORMATION -> 5;
            case VOID_REFINEMENT -> 9;
            case UNITY -> 12;
            case MAHAYANA -> 18;
            case TRIBULATION, TRUE_IMMORTAL -> 27;
            default -> 0;
        };
    }

    private static Snapshot loadBuiltin() {
        Map<String, Rule> byId = new LinkedHashMap<>();
        Map<Realm, Rule> byRealm = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/tribulation_rules.json");
        if (root == null) {
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/tribulation_rules_index.json");
        }
        if (root != null) {
            JsonArray array = root.has("types") && root.get("types").isJsonArray()
                    ? root.getAsJsonArray("types")
                    : (root.has("entries") && root.get("entries").isJsonArray()
                    ? root.getAsJsonArray("entries") : new JsonArray());
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                String trigger = firstNonBlank(str(o, "trigger_realm"),
                        o.has("setting") && o.get("setting").isJsonObject()
                                ? str(o.getAsJsonObject("setting"), "trigger_realm") : "");
                int waves = o.has("waves") && o.get("waves").isJsonPrimitive() ? o.get("waves").getAsInt() : 0;
                double damage = o.has("damage_per_wave_base") && o.get("damage_per_wave_base").isJsonPrimitive()
                        ? o.get("damage_per_wave_base").getAsDouble() : 0.0D;
                String tier = str(o, "tribulation_tier");
                Rule rule = new Rule(id, firstNonBlank(str(o, "display"), id), trigger, waves, damage, tier);
                byId.put(id, rule);
                Realm realm = Realm.fromDesignId(trigger);
                if (realm != null && waves > 0) {
                    byRealm.putIfAbsent(realm, rule);
                }
            }
        }
        if (byId.isEmpty()) {
            put(byId, byRealm, "minor_soul_trial", "元婴小劫", "NASCENT_SOUL", 3, 40.0D, "minor");
            put(byId, byRealm, "heart_demon_or_thunder", "化神心魔/雷劫", "DEITY_TRANSFORMATION", 5, 80.0D, "major");
            put(byId, byRealm, "void_thunder", "炼虚雷劫", "VOID_REFINEMENT", 9, 120.0D, "major");
            put(byId, byRealm, "body_soul_dual", "合体双劫", "BODY_INTEGRATION", 12, 160.0D, "major");
            put(byId, byRealm, "great_ascension_thunder", "大乘天劫", "GREAT_VEHICLE", 18, 220.0D, "major");
            put(byId, byRealm, "final_ascension", "飞升仙劫", "TRIBULATION_LAND", 27, 300.0D, "immortal");
        }
        return new Snapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(byRealm));
    }

    private static void put(Map<String, Rule> byId, Map<Realm, Rule> byRealm,
                            String id, String display, String trigger, int waves, double damage, String tier) {
        Rule rule = new Rule(id, display, trigger, waves, damage, tier);
        byId.put(id, rule);
        Realm realm = Realm.fromDesignId(trigger);
        if (realm != null) byRealm.put(realm, rule);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TribulationRulesCatalog.class.getClassLoader().getResourceAsStream(path)) {
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
