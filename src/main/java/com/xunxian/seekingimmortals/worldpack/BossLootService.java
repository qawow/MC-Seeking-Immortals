package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
 * M09 boss loot authority.
 * <p>Redline: unique / first_clear_only drops never appear on repeat clears.
 * Unique catalog items ({@link ItemCatalogService#isUniqueForbidden}) are always blocked.</p>
 */
public final class BossLootService {
    private static final Snapshot SNAPSHOT = load();

    private BossLootService() {}

    public record DropDef(
            String itemId,
            double chance,
            int countMin,
            int countMax,
            boolean unique,
            boolean firstClearOnly) {}

    public record TableDef(String bossId, String secretRealmId, List<DropDef> drops) {}

    public record Snapshot(Map<String, TableDef> byBossId) {
        public int size() {
            return byBossId.size();
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<TableDef> find(String bossId) {
        if (bossId == null || bossId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.byBossId().get(bossId.trim().toLowerCase(Locale.ROOT)));
    }

    public static List<DropDef> dropsFor(String bossId, boolean firstClear) {
        Optional<TableDef> table = find(bossId);
        if (table.isEmpty()) {
            return List.of();
        }
        List<DropDef> out = new ArrayList<>();
        for (DropDef drop : table.get().drops()) {
            if (!firstClear && (drop.unique() || drop.firstClearOnly())) {
                continue;
            }
            if (isForbidden(drop.itemId())) {
                continue;
            }
            out.add(drop);
        }
        return List.copyOf(out);
    }

    /**
     * Roll and grant loot. Returns number of stacks granted.
     */
    public static int grantBossLoot(ServerPlayer player, String bossId, boolean firstClear, RandomSource random) {
        if (player == null || bossId == null || bossId.isBlank()) {
            return 0;
        }
        RandomSource rng = random == null ? player.getRandom() : random;
        List<DropDef> drops = dropsFor(bossId, firstClear);
        int granted = 0;
        for (DropDef drop : drops) {
            if (rng.nextDouble() > Math.max(0.0D, Math.min(1.0D, drop.chance()))) {
                continue;
            }
            int min = Math.max(1, drop.countMin());
            int max = Math.max(min, drop.countMax());
            int count = min == max ? min : min + rng.nextInt(max - min + 1);
            Optional<ItemStack> stack = resolveStack(drop.itemId(), count);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack gift = stack.get();
            if (!player.getInventory().add(gift.copy())) {
                player.drop(gift.copy(), false);
            }
            granted++;
        }
        if (granted == 0) {
            // Soft fallback so bosses never feel empty on a bad roll.
            ItemStack fallback = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), firstClear ? 6 : 3);
            if (!player.getInventory().add(fallback.copy())) {
                player.drop(fallback.copy(), false);
            }
            granted = 1;
        }
        return granted;
    }

    public static boolean isForbidden(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return true;
        }
        try {
            return ItemCatalogService.isUniqueForbidden(itemId);
        } catch (Throwable ignored) {
            String key = itemId.toLowerCase(Locale.ROOT);
            return key.contains("palm_heaven") || key.contains("green_liquid") || key.contains("lv_ye");
        }
    }

    public static Optional<ItemStack> resolveStack(String raw, int count) {
        if (raw == null || raw.isBlank() || isForbidden(raw)) {
            return Optional.empty();
        }
        String id = raw.trim();
        if (id.contains(":")) {
            id = id.substring(id.indexOf(':') + 1);
        }
        try {
            Item item = ItemCatalogService.resolveCatalogItem(id);
            if (item == null) {
                return Optional.empty();
            }
            return Optional.of(new ItemStack(item, Math.max(1, count)));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static Snapshot load() {
        Map<String, TableDef> byId = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/worldpack/boss_loot_runtime.json");
        if (root == null) {
            // Fallback to text_material design tables if runtime pack missing.
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/boss_loot_tables.json");
        }
        if (root == null) {
            return new Snapshot(Collections.emptyMap());
        }
        JsonArray tables = root.has("tables") && root.get("tables").isJsonArray()
                ? root.getAsJsonArray("tables")
                : new JsonArray();
        for (JsonElement element : tables) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String bossId = str(object, "boss_id");
            if (bossId.isBlank()) {
                bossId = str(object, "id");
            }
            if (bossId.isBlank()) {
                continue;
            }
            bossId = bossId.trim().toLowerCase(Locale.ROOT);
            List<DropDef> drops = new ArrayList<>();
            for (JsonElement dropEl : array(object, "drops")) {
                if (!dropEl.isJsonObject()) {
                    continue;
                }
                JsonObject drop = dropEl.getAsJsonObject();
                String item = str(drop, "item");
                if (item.isBlank()) {
                    continue;
                }
                boolean unique = bool(drop, "unique", false) || bool(drop, "first_clear_only", false) || bool(drop, "once", false);
                if (looksUnique(item)) {
                    unique = true;
                }
                int countMin = 1;
                int countMax = 1;
                if (drop.has("count_min") && drop.get("count_min").isJsonPrimitive()) {
                    countMin = Math.max(1, drop.get("count_min").getAsInt());
                    countMax = Math.max(countMin, intValue(drop, "count_max", countMin));
                } else if (drop.has("count") && drop.get("count").isJsonArray()) {
                    JsonArray arr = drop.getAsJsonArray("count");
                    if (arr.size() >= 1 && arr.get(0).isJsonPrimitive()) {
                        countMin = Math.max(1, arr.get(0).getAsInt());
                    }
                    if (arr.size() >= 2 && arr.get(1).isJsonPrimitive()) {
                        countMax = Math.max(countMin, arr.get(1).getAsInt());
                    } else {
                        countMax = countMin;
                    }
                } else if (drop.has("count") && drop.get("count").isJsonPrimitive()) {
                    countMin = countMax = Math.max(1, drop.get("count").getAsInt());
                } else {
                    countMax = Math.max(countMin, intValue(drop, "count_max", countMin));
                }
                drops.add(new DropDef(
                        item,
                        doubleValue(drop, "chance", 0.1D),
                        countMin,
                        countMax,
                        unique,
                        unique || bool(drop, "first_clear_only", false)));
            }
            byId.put(bossId, new TableDef(bossId, str(object, "secret_realm"), List.copyOf(drops)));
        }
        SeekingImmortalsMod.LOGGER.info("M09 boss loot tables loaded: {}", byId.size());
        return new Snapshot(Collections.unmodifiableMap(byId));
    }

    private static boolean looksUnique(String item) {
        String key = item == null ? "" : item.toLowerCase(Locale.ROOT);
        return key.contains("void_key")
                || key.contains("qingning_mirror")
                || key.contains("palm")
                || key.contains("green_liquid")
                || key.contains("great_shift_token")
                || key.contains("xuanguang")
                || key.contains("xuanhuang");
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BossLootService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static double doubleValue(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsDouble()
                : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isBoolean()
                ? object.get(key).getAsBoolean()
                : fallback;
    }
}
