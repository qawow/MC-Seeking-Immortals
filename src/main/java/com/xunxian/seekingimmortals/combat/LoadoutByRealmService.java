package com.xunxian.seekingimmortals.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStageConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * loadout_by_realm_v99 平衡基准读取，供 CombatStats 校准断言使用。
 */
public final class LoadoutByRealmService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private LoadoutByRealmService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record LoadoutEntry(
            String realmDesignId,
            String display,
            double priceBandLow,
            double priceBandHigh) {
    }

    /**
     * 同境界期望伤害区间：基于 RealmStageConfig 攻击/防御公式的确定性基线。
     * <p>raw = attack；mitigation = def/(def+100)；final = max(1, raw * (1 - mitigation))</p>
     */
    public record DamageBaseline(
            Realm realm,
            double attackBase,
            double defenseBase,
            double expectedMinDamage,
            double expectedMaxDamage) {
    }

    public record Snapshot(Map<String, LoadoutEntry> byRealmDesignId) {
        public Optional<LoadoutEntry> find(String realmDesignId) {
            if (realmDesignId == null || realmDesignId.isBlank()) {
                return Optional.empty();
            }
            Realm realm = Realm.fromDesignId(realmDesignId);
            if (realm != null) {
                LoadoutEntry byEnum = byRealmDesignId.get(realm.getDesignId());
                if (byEnum != null) {
                    return Optional.of(byEnum);
                }
            }
            return Optional.ofNullable(byRealmDesignId.get(realmDesignId.trim().toUpperCase()));
        }

        public int size() {
            return byRealmDesignId.size();
        }
    }

    public static DamageBaseline damageBaseline(Realm realm) {
        double attack = RealmStageConfig.getAttackBase(realm);
        double defense = RealmStageConfig.getDefenseBase(realm);
        double reduction = defense / (defense + 100.0D);
        double mid = Math.max(1.0D, attack * (1.0D - reduction));
        // 允许约 ±15% 数值漂移窗口（crit 未计入；同配置无暴击基线）
        return new DamageBaseline(realm, attack, defense, mid * 0.85D, mid * 1.15D);
    }

    public static double expectedMitigatedDamage(Realm attackerRealm, Realm defenderRealm) {
        double attack = RealmStageConfig.getAttackBase(attackerRealm);
        double defense = RealmStageConfig.getDefenseBase(defenderRealm);
        double reduction = defense / (defense + 100.0D);
        return Math.max(1.0D, attack * (1.0D - reduction));
    }

    private static Snapshot loadBuiltin() {
        Map<String, LoadoutEntry> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/loadout_by_realm_v99.json");
        if (root != null && root.has("loadouts") && root.get("loadouts").isJsonArray()) {
            JsonArray array = root.getAsJsonArray("loadouts");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String realmId = str(o, "realm").toUpperCase();
                if (realmId.isBlank()) continue;
                double low = 0.0D;
                double high = 0.0D;
                if (o.has("price_band_low_stone") && o.get("price_band_low_stone").isJsonArray()) {
                    JsonArray band = o.getAsJsonArray("price_band_low_stone");
                    if (band.size() >= 1) low = band.get(0).getAsDouble();
                    if (band.size() >= 2) high = band.get(1).getAsDouble();
                }
                map.put(realmId, new LoadoutEntry(realmId, firstNonBlank(str(o, "display"), realmId), low, high));
            }
        }
        return new Snapshot(Collections.unmodifiableMap(map));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = LoadoutByRealmService.class.getClassLoader().getResourceAsStream(path)) {
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
