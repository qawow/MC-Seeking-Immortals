package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.Realm;

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
 * M10: thirteen-tier beast schema + realm mapping (M01 design ids).
 * Stats scale from tier via realm-equivalent baselines.
 */
public final class BeastTierService {
    private static final Map<Integer, TierDef> TIERS = loadTiers();
    private static final int DEMON_CORE_FROM_TIER = 5;
    private static final int SHAPE_SHIFT_AT_TIER = 7;

    private BeastTierService() {}

    public record TierDef(int tier, String display, String realmEquiv, String lootBand) {}

    public record ScaledStats(double health, double damage, double armor, int lifeTicks) {}

    public static int tierCount() {
        return TIERS.size();
    }

    public static Map<Integer, TierDef> tiers() {
        return TIERS;
    }

    public static Optional<TierDef> tier(int tier) {
        return Optional.ofNullable(TIERS.get(clampTier(tier)));
    }

    public static int demonCoreFromTier() {
        return DEMON_CORE_FROM_TIER;
    }

    public static int shapeShiftAtTier() {
        return SHAPE_SHIFT_AT_TIER;
    }

    public static boolean hasDemonCore(int tier) {
        return clampTier(tier) >= DEMON_CORE_FROM_TIER;
    }

    public static String lootBandFor(int tier) {
        int t = clampTier(tier);
        if (t <= 4) {
            return "1-4";
        }
        if (t <= 8) {
            return "5-8";
        }
        return "9-13";
    }

    /**
     * Map a corpus realm_equiv token onto a M01 {@link Realm}.
     * Tokens may include stage suffixes (EARLY/MID/LATE/PEAK) which are ignored for ordinal gates.
     */
    public static Realm realmForTier(int tier) {
        TierDef def = TIERS.get(clampTier(tier));
        if (def == null) {
            return Realm.QI_REFINING;
        }
        return parseRealm(def.realmEquiv());
    }

    public static Realm parseRealm(String realmEquiv) {
        if (realmEquiv == null || realmEquiv.isBlank()) {
            return Realm.QI_REFINING;
        }
        String raw = realmEquiv.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        // Strip stage suffixes so Realm.fromDesignId can match the major realm.
        String major = raw
                .replace("_EARLY", "")
                .replace("_MID", "")
                .replace("_LATE", "")
                .replace("_PEAK", "")
                .replace("_TO_GREAT", "")
                .replace("DEITY_TO_GREAT", "DEITY_TRANSFORMATION")
                .replace("SPIRIT_TRANSFORMATION", "DEITY_TRANSFORMATION");
        if (major.startsWith("FOUNDATION")) {
            major = "FOUNDATION";
        } else if (major.startsWith("QI_REFINING") || major.startsWith("QI")) {
            major = "QI_REFINING";
        } else if (major.startsWith("CORE")) {
            major = "CORE_FORMATION";
        } else if (major.startsWith("NASCENT")) {
            major = "NASCENT_SOUL";
        } else if (major.startsWith("DEITY") || major.startsWith("SOUL_TRANS") || major.startsWith("SPIRIT")) {
            major = "DEITY_TRANSFORMATION";
        } else if (major.startsWith("VOID")) {
            major = "VOID_REFINEMENT";
        } else if (major.startsWith("BODY") || major.startsWith("UNITY") || major.contains("INTEGRATION")) {
            major = "BODY_INTEGRATION";
        } else if (major.startsWith("GREAT") || major.startsWith("MAHAYANA")) {
            major = "GREAT_VEHICLE";
        }
        Realm realm = Realm.fromDesignId(major);
        return realm == null ? Realm.QI_REFINING : realm;
    }

    /**
     * Cross-band suppression: attacker tier must not lag defender by more than {@code maxGap}.
     * Server-side contract / capture / boss gates use this.
     */
    public static boolean canSuppress(int actorTier, int targetTier, int maxGap) {
        return clampTier(actorTier) + Math.max(0, maxGap) >= clampTier(targetTier);
    }

    /**
     * Scale combat stats from tier. Base follows M01 realm ordinal growth.
     */
    public static ScaledStats scaleStats(int tier) {
        int t = clampTier(tier);
        Realm realm = realmForTier(t);
        double realmFactor = 1.0D + realm.ordinal() * 0.35D;
        double health = (16.0D + t * 8.0D) * realmFactor;
        double damage = (3.0D + t * 1.6D) * (0.85D + realm.ordinal() * 0.12D);
        double armor = Math.min(20.0D, 1.0D + t * 0.9D + realm.ordinal());
        int life = 20 * (40 + t * 12 + realm.ordinal() * 8);
        return new ScaledStats(health, damage, armor, life);
    }

    public static int clampTier(int tier) {
        if (tier < 1) {
            return 1;
        }
        if (tier > 13) {
            return 13;
        }
        return tier;
    }

    private static Map<Integer, TierDef> loadTiers() {
        Map<Integer, TierDef> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_thirteen_tier_map.json");
        if (root == null) {
            // Hardcoded fallback so pure unit tests still see 13 tiers when resources are absent.
            for (int i = 1; i <= 13; i++) {
                map.put(i, fallback(i));
            }
            return Collections.unmodifiableMap(map);
        }
        JsonArray array = root.has("tiers") && root.get("tiers").isJsonArray()
                ? root.getAsJsonArray("tiers") : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            int tier = o.has("tier") ? o.get("tier").getAsInt() : 0;
            if (tier < 1 || tier > 13) {
                continue;
            }
            map.put(tier, new TierDef(
                    tier,
                    str(o, "display"),
                    str(o, "realm_equiv"),
                    str(o, "loot_band")));
        }
        for (int i = 1; i <= 13; i++) {
            map.putIfAbsent(i, fallback(i));
        }
        return Collections.unmodifiableMap(map);
    }

    private static TierDef fallback(int tier) {
        String band = lootBandFor(tier);
        String realm = switch (tier) {
            case 1 -> "QI_REFINING_EARLY";
            case 2 -> "QI_REFINING_MID";
            case 3 -> "QI_REFINING_LATE";
            case 4 -> "QI_REFINING_PEAK";
            case 5 -> "FOUNDATION_EARLY";
            case 6 -> "FOUNDATION_MID";
            case 7 -> "FOUNDATION_LATE";
            case 8 -> "FOUNDATION_PEAK";
            case 9 -> "CORE_FORMATION_EARLY";
            case 10 -> "CORE_FORMATION_LATE";
            case 11 -> "NASCENT_SOUL_EARLY";
            case 12 -> "NASCENT_SOUL";
            default -> "SPIRIT_TRANSFORMATION";
        };
        return new TierDef(tier, tier + "阶妖兽", realm, band);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BeastTierService.class.getClassLoader().getResourceAsStream(path)) {
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
