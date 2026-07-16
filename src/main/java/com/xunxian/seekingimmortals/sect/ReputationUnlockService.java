package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M08 reputation threshold → unlock query API (shop tiers / quest lines / region access tokens).
 * Runtime reputation values still live in {@link ReputationService}; this service only interprets corpus thresholds.
 */
public final class ReputationUnlockService {
    public static final String TIER_HOSTILE = "hostile";
    public static final String TIER_UNFRIENDLY = "unfriendly";
    public static final String TIER_NEUTRAL = "neutral";
    public static final String TIER_FRIENDLY = "friendly";
    public static final String TIER_HONORED = "honored";
    public static final String TIER_EXALTED = "exalted";

    private static final Snapshot BUILTIN = loadBuiltin();

    private ReputationUnlockService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<FactionUnlocks> find(String factionId) {
        return Optional.ofNullable(BUILTIN.factions().get(canonicalize(factionId)));
    }

    public static String tierFor(int reputation) {
        int value = clamp(reputation);
        if (inRange(value, BUILTIN.scale().hostile())) return TIER_HOSTILE;
        if (inRange(value, BUILTIN.scale().unfriendly())) return TIER_UNFRIENDLY;
        if (inRange(value, BUILTIN.scale().neutral())) return TIER_NEUTRAL;
        if (inRange(value, BUILTIN.scale().friendly())) return TIER_FRIENDLY;
        if (inRange(value, BUILTIN.scale().honored())) return TIER_HONORED;
        if (inRange(value, BUILTIN.scale().exalted())) return TIER_EXALTED;
        return TIER_NEUTRAL;
    }

    public static List<String> unlockedFor(String factionId, int reputation) {
        FactionUnlocks faction = BUILTIN.factions().get(canonicalize(factionId));
        if (faction == null) {
            return List.of();
        }
        Set<String> unlocked = new LinkedHashSet<>();
        for (Threshold threshold : faction.thresholds()) {
            if (threshold.at() >= 0 && reputation >= threshold.at()) {
                unlocked.addAll(threshold.unlocks());
            }
        }
        return List.copyOf(unlocked);
    }

    public static List<String> lockedFor(String factionId, int reputation) {
        FactionUnlocks faction = BUILTIN.factions().get(canonicalize(factionId));
        if (faction == null) {
            return List.of();
        }
        Set<String> locked = new LinkedHashSet<>();
        for (Threshold threshold : faction.thresholds()) {
            if (threshold.at() < 0 && reputation <= threshold.at()) {
                locked.addAll(threshold.locks());
            }
            // Positive thresholds not yet reached stay locked as unlock labels.
            if (threshold.at() >= 0 && reputation < threshold.at()) {
                locked.addAll(threshold.unlocks());
            }
        }
        return List.copyOf(locked);
    }

    public static boolean isUnlocked(String factionId, int reputation, String unlockToken) {
        String token = normalize(unlockToken);
        if (token.isBlank()) {
            return true;
        }
        for (String unlocked : unlockedFor(factionId, reputation)) {
            if (normalize(unlocked).equals(token) || normalize(unlocked).contains(token) || token.contains(normalize(unlocked))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRegionAccessOpen(String factionId, int reputation) {
        // Hostile lock tokens that mention 山门/追拿 close region access.
        for (String lock : lockedFor(factionId, reputation)) {
            String n = normalize(lock);
            if (n.contains("山门") || n.contains("gate") || n.contains("追拿") || n.contains("hunt") || n.contains("ban")) {
                return false;
            }
        }
        return reputation > BUILTIN.scale().hostile()[1];
    }

    public static boolean isShopTierOpen(String factionId, int reputation, int requiredRep) {
        return reputation >= requiredRep && isRegionAccessOpen(factionId, reputation);
    }

    public static List<String> unlockedForPlayer(ServerPlayer player, String factionId) {
        if (player == null) {
            return List.of();
        }
        String key = reputationKey(factionId);
        return unlockedFor(factionId, ReputationService.get(player, key));
    }

    public static String reputationKey(String factionId) {
        String id = canonicalize(factionId);
        FactionUnlocks faction = BUILTIN.factions().get(id);
        if (faction != null && !faction.reputationKey().isBlank()) {
            return faction.reputationKey();
        }
        // Align with existing ReputationService keys used by travel/shop hooks.
        return switch (id) {
            case "huangfeng", "huangfeng_valley" -> "dajin";
            case "star_palace", "inverse_star", "inverse_star_alliance" -> "chaotic_sea";
            case "mulan", "mulan_fashi_council", "tianlan", "tianlan_temple" -> "mulan";
            case "guiling", "guiling_men", "guiling_gate", "ghost_spirit_gate" -> "demonic_path";
            case "tianyuan", "tianyuan_city" -> "tianyuan";
            default -> id;
        };
    }

    private static Snapshot loadBuiltin() {
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/reputation_unlocks_v102.json");
        Scale scale = Scale.defaults();
        Map<String, FactionUnlocks> factions = new LinkedHashMap<>();
        Map<String, String> globalRules = new LinkedHashMap<>();
        if (root == null) {
            return new Snapshot(scale, Map.of(), Map.of());
        }
        if (root.has("scale") && root.get("scale").isJsonObject()) {
            JsonObject s = root.getAsJsonObject("scale");
            scale = new Scale(
                    intOr(s, "min", -100),
                    intOr(s, "max", 100),
                    range(s, "hostile", -100, -40),
                    range(s, "unfriendly", -39, -10),
                    range(s, "neutral", -9, 20),
                    range(s, "friendly", 21, 50),
                    range(s, "honored", 51, 80),
                    range(s, "exalted", 81, 100));
        }
        if (root.has("global_rules") && root.get("global_rules").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("global_rules").entrySet()) {
                globalRules.put(entry.getKey(), entry.getValue().isJsonNull() ? "" : entry.getValue().toString());
            }
        }
        JsonArray arr = root.getAsJsonArray("factions");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String id = canonicalize(str(o, "id"));
                if (id.isBlank()) continue;
                List<String> gains = stringList(o.get("gains"));
                List<String> losses = stringList(o.get("losses"));
                List<Threshold> thresholds = new ArrayList<>();
                JsonArray th = o.getAsJsonArray("thresholds");
                if (th != null) {
                    for (JsonElement tEl : th) {
                        if (!tEl.isJsonObject()) continue;
                        JsonObject t = tEl.getAsJsonObject();
                        int at = intOr(t, "at", 0);
                        thresholds.add(new Threshold(
                                at,
                                stringList(t.get("unlock")),
                                stringList(t.get("lock")),
                                str(t, "effect"),
                                str(t, "note")));
                    }
                }
                FactionUnlocks unlocks = new FactionUnlocks(id, str(o, "display"), id, gains, losses, List.copyOf(thresholds));
                factions.put(id, unlocks);
                // Alias common corpus sect ids onto unlock table ids.
                for (String alias : aliasesOf(id)) {
                    factions.putIfAbsent(alias, unlocks);
                }
            }
        }
        return new Snapshot(scale, Collections.unmodifiableMap(factions), Collections.unmodifiableMap(globalRules));
    }

    private static List<String> aliasesOf(String id) {
        return switch (id) {
            case "huangfeng" -> List.of("huangfeng_valley", "sect_huangfeng");
            case "yueling_qipai" -> List.of("yue_seven", "tiannan_seven");
            case "guiling_men" -> List.of("guiling_gate", "ghost_spirit_gate", "guiling");
            case "star_palace" -> List.of("xinggong");
            case "inverse_star" -> List.of("inverse_star_alliance");
            case "mulan" -> List.of("mulan_fashi_council");
            case "tianlan" -> List.of("tianlan_temple");
            default -> List.of();
        };
    }

    private static String canonicalize(String factionId) {
        String id = normalize(factionId);
        if (id.startsWith("sect_")) {
            id = id.substring("sect_".length());
        }
        return switch (id) {
            case "huangfeng_valley" -> "huangfeng";
            case "guiling_gate", "ghost_spirit_gate", "guiling" -> "guiling_men";
            case "inverse_star_alliance" -> "inverse_star";
            case "mulan_fashi_council" -> "mulan";
            case "tianlan_temple" -> "tianlan";
            default -> id;
        };
    }

    private static int[] range(JsonObject scale, String key, int lo, int hi) {
        if (scale == null || !scale.has(key) || !scale.get(key).isJsonArray()) {
            return new int[]{lo, hi};
        }
        JsonArray arr = scale.getAsJsonArray(key);
        if (arr.size() < 2) {
            return new int[]{lo, hi};
        }
        return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()};
    }

    private static boolean inRange(int value, int[] range) {
        return range != null && range.length >= 2 && value >= range[0] && value <= range[1];
    }

    private static int clamp(int reputation) {
        return Math.max(BUILTIN.scale().min(), Math.min(BUILTIN.scale().max(), reputation));
    }

    private static List<String> stringList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return list;
        }
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = ReputationUnlockService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load reputation unlocks {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Scale(int min, int max, int[] hostile, int[] unfriendly, int[] neutral,
                        int[] friendly, int[] honored, int[] exalted) {
        static Scale defaults() {
            return new Scale(-100, 100,
                    new int[]{-100, -40}, new int[]{-39, -10}, new int[]{-9, 20},
                    new int[]{21, 50}, new int[]{51, 80}, new int[]{81, 100});
        }
    }

    public record Threshold(int at, List<String> unlocks, List<String> locks, String effect, String note) {}

    public record FactionUnlocks(String id, String display, String reputationKey,
                                 List<String> gains, List<String> losses, List<Threshold> thresholds) {}

    public record Snapshot(Scale scale, Map<String, FactionUnlocks> factions, Map<String, String> globalRules) {
        public int factionCount() {
            // Unique canonical rows roughly half of alias-expanded map; expose raw size for tests.
            return factions.size();
        }
    }
}
