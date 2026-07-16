package com.xunxian.seekingimmortals.combat.status;

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
 * 读取权威 {@code status_effects.json}（作者侧 + 发布副本）。
 */
public final class StatusCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private StatusCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record StatusDefinition(
            String id,
            String display,
            String displayEn,
            boolean beneficial,
            String family,
            int colorRgb,
            int maxAmplifier,
            int defaultDurationTicks,
            double tickDamage,
            double tickHeal,
            int tickInterval,
            double movementMul,
            double outgoingDamageMul,
            double defenseMul,
            boolean blocksTechnique,
            boolean hidesRealm,
            List<String> aliases) {
    }

    public record PoisonVariant(String id, String statusId, String display) {}

    public record AntidoteClear(
            String id,
            List<String> clearsFamilies,
            List<String> clearsVariants,
            List<String> clearsStatusIds,
            List<String> failsOn,
            boolean emergency) {}

    public record Snapshot(
            Map<String, StatusDefinition> byId,
            Map<String, String> aliasToId,
            List<PoisonVariant> poisonVariants,
            List<AntidoteClear> antidotes) {
        public List<StatusDefinition> effects() {
            return List.copyOf(byId.values());
        }

        public Optional<StatusDefinition> find(String idOrAlias) {
            if (idOrAlias == null || idOrAlias.isBlank()) {
                return Optional.empty();
            }
            String key = idOrAlias.trim().toLowerCase(Locale.ROOT);
            StatusDefinition direct = byId.get(key);
            if (direct != null) {
                return Optional.of(direct);
            }
            String mapped = aliasToId.get(key);
            return mapped == null ? Optional.empty() : Optional.ofNullable(byId.get(mapped));
        }

        public int size() {
            return byId.size();
        }

        public List<String> ids() {
            return List.copyOf(byId.keySet());
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, StatusDefinition> byId = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        List<PoisonVariant> poisons = new ArrayList<>();
        List<AntidoteClear> antidotes = new ArrayList<>();

        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/status_effects.json");
        if (root != null) {
            JsonArray effects = root.has("effects") && root.get("effects").isJsonArray()
                    ? root.getAsJsonArray("effects") : new JsonArray();
            for (JsonElement element : effects) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                String category = str(o, "category").toLowerCase(Locale.ROOT);
                boolean beneficial = "beneficial".equals(category);
                List<String> aliasList = stringList(o, "aliases");
                StatusDefinition def = new StatusDefinition(
                        id,
                        firstNonBlank(str(o, "display"), id),
                        firstNonBlank(str(o, "display_en"), id),
                        beneficial,
                        firstNonBlank(str(o, "family"), "misc"),
                        parseColor(str(o, "color"), beneficial ? 0x55FF55 : 0xAA0000),
                        Math.max(0, intOr(o, "max_amplifier", 3)),
                        Math.max(1, intOr(o, "default_duration_ticks", 100)),
                        doubleOr(o, "tick_damage", 0.0D),
                        doubleOr(o, "tick_heal", 0.0D),
                        Math.max(1, intOr(o, "tick_interval", 20)),
                        o.has("movement_mul") ? doubleOr(o, "movement_mul", 1.0D) : 1.0D,
                        o.has("outgoing_damage_mul") ? doubleOr(o, "outgoing_damage_mul", 1.0D) : 1.0D,
                        o.has("defense_mul") ? doubleOr(o, "defense_mul", 1.0D) : 1.0D,
                        boolOr(o, "blocks_technique", false),
                        boolOr(o, "hides_realm", false),
                        aliasList);
                byId.put(id, def);
                for (String alias : aliasList) {
                    if (alias != null && !alias.isBlank()) {
                        aliases.put(alias.trim().toLowerCase(Locale.ROOT), id);
                    }
                }
            }

            JsonArray poisonArr = root.has("poison_variants") && root.get("poison_variants").isJsonArray()
                    ? root.getAsJsonArray("poison_variants") : new JsonArray();
            for (JsonElement element : poisonArr) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                String statusId = str(o, "status_id").toLowerCase(Locale.ROOT);
                if (id.isBlank() || statusId.isBlank()) continue;
                poisons.add(new PoisonVariant(id, statusId, firstNonBlank(str(o, "display"), id)));
            }

            JsonArray antidoteArr = root.has("antidote_clears") && root.get("antidote_clears").isJsonArray()
                    ? root.getAsJsonArray("antidote_clears") : new JsonArray();
            for (JsonElement element : antidoteArr) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank()) continue;
                antidotes.add(new AntidoteClear(
                        id,
                        stringList(o, "clears_families"),
                        stringList(o, "clears_variants"),
                        stringList(o, "clears_status_ids"),
                        stringList(o, "fails_on"),
                        boolOr(o, "emergency", false)));
            }
        }

        if (byId.isEmpty()) {
            putFallback(byId, "burn", "灼烧", false, 0xFF6A00);
            putFallback(byId, "frozen", "冻结", false, 0x7EC8FF);
            putFallback(byId, "soul_shock", "神魂震击", false, 0xC9A0FF);
            putFallback(byId, "illusion", "幻惑", false, 0xE0A0FF);
            putFallback(byId, "karma", "业力", false, 0x8B0000);
            putFallback(byId, "demonic_qi", "魔气侵染", false, 0x4B0082);
            putFallback(byId, "foundation_unstable", "根基不稳", false, 0xA08060);
            putFallback(byId, "marrow_drain", "抽髓", false, 0xB03060);
            putFallback(byId, "seal_nascent", "封婴", false, 0x606080);
            putFallback(byId, "conceal_qi", "敛息", true, 0x708090);
            putFallback(byId, "poison", "中毒", false, 0x3CB371);
            putFallback(byId, "bleed", "流血", false, 0xDC143C);
            putFallback(byId, "stun", "定身", false, 0xFFD700);
            putFallback(byId, "shield", "护体", true, 0x87CEEB);
            putFallback(byId, "fear", "心悸", false, 0x6A0DAD);
            putFallback(byId, "berserk", "狂化", true, 0xFF2400);
            putFallback(byId, "qi_disorder", "气机紊乱", false, 0x708070);
            putFallback(byId, "soul_wound", "魂伤", false, 0x9370DB);
            putFallback(byId, "sword_intent", "剑意", true, 0xB0C4DE);
            putFallback(byId, "array_bind", "阵锁", false, 0x4682B4);
            putFallback(byId, "heal_hot", "回春", true, 0x32CD32);
            putFallback(byId, "tribulation_mark", "劫痕", false, 0x2F4F4F);
            aliases.put("freeze", "frozen");
        }

        return new Snapshot(
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(aliases),
                List.copyOf(poisons),
                List.copyOf(antidotes));
    }

    private static void putFallback(Map<String, StatusDefinition> map, String id, String display,
                                    boolean beneficial, int color) {
        map.put(id, new StatusDefinition(
                id, display, id, beneficial, "misc", color, 3, 100,
                "burn".equals(id) || "poison".equals(id) || "bleed".equals(id) ? 1.0D : 0.0D,
                "heal_hot".equals(id) ? 1.0D : 0.0D,
                20, 1.0D, 1.0D, 1.0D,
                "seal_nascent".equals(id),
                "conceal_qi".equals(id),
                List.of()));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = StatusCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> out = new ArrayList<>();
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return out;
        }
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element != null && element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value != null && !value.isBlank()) {
                    out.add(value.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return out;
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

    private static int intOr(JsonObject object, String key, int fallback) {
        try {
            if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
                return object.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        try {
            if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
                return object.get(key).getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static boolean boolOr(JsonObject object, String key, boolean fallback) {
        try {
            if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
                return object.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static int parseColor(String color, int fallback) {
        if (color == null || color.isBlank()) return fallback;
        String cleaned = color.trim();
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }
        try {
            return (int) Long.parseLong(cleaned, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return fallback;
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
