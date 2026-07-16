package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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
 * M10: beast loot resolution from {@code beast_loot_tiers} + {@code beast_materials_loot_v92}.
 * Material ids resolve through M03 {@link ItemCatalogService}.
 */
public final class BeastLootService {
    /** Chinese / alias → catalog id. Must initialize before SNAPSHOT (load uses resolveItemId). */
    private static final Map<String, String> DISPLAY_ALIASES = Map.ofEntries(
            Map.entry("兽皮", "beast_hide"),
            Map.entry("厚兽皮", "beast_hide_thick"),
            Map.entry("兽骨", "beast_bone_block"),
            Map.entry("劣质兽血", "beast_blood_vial"),
            Map.entry("低阶兽筋", "beast_tendon"),
            Map.entry("兽筋", "beast_tendon"),
            Map.entry("妖丹", "demon_core_low"),
            Map.entry("低阶妖丹", "demon_core_low"),
            Map.entry("中阶妖丹", "demon_core_mid"),
            Map.entry("高阶妖丹", "demon_core_high"),
            Map.entry("妖丹碎片", "demon_core_fragment"),
            Map.entry("兽核", "beast_core"),
            Map.entry("灵兽骨", "spirit_beast_bone"),
            Map.entry("兽魂精华", "beast_soul_essence"),
            Map.entry("真灵血滴", "true_spirit_blood_drop"),
            Map.entry("蛟鳞", "jiao_scale"),
            Map.entry("龙鳞", "dragon_scale")
    );

    private static final Snapshot SNAPSHOT = load();

    private BeastLootService() {}

    public record DropSpec(String itemId, double chance, int minCount, int maxCount) {}

    public record Snapshot(
            Map<String, List<DropSpec>> bandDrops,
            Map<Integer, List<String>> tierCommon,
            Map<Integer, List<String>> tierUncommon,
            Map<Integer, List<String>> tierRare,
            Map<String, List<String>> namedDrops) {}

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static String resolveItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        String alias = DISPLAY_ALIASES.get(trimmed);
        if (alias != null) {
            return alias;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT).replace(' ', '_');
        // Prefer catalog canonical id when known.
        try {
            String resolved = ItemCatalogService.resolveId(lower);
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
        } catch (Throwable ignored) {
            // unit tests without full catalog
        }
        return lower;
    }

    public static Optional<ItemStack> resolveStack(String raw, int count) {
        String id = resolveItemId(raw);
        if (id.isBlank()) {
            return Optional.empty();
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

    public static List<DropSpec> dropsForTier(int tier) {
        String band = BeastTierService.lootBandFor(tier);
        List<DropSpec> list = SNAPSHOT.bandDrops().getOrDefault(band, List.of());
        if (!list.isEmpty()) {
            return list;
        }
        // Fallback synthetic band.
        List<DropSpec> synth = new ArrayList<>();
        synth.add(new DropSpec("beast_blood_vial", 0.4D, 1, 2));
        if (tier >= BeastTierService.demonCoreFromTier()) {
            synth.add(new DropSpec(tier >= 9 ? "demon_core_high" : (tier >= 5 ? "demon_core_mid" : "demon_core_low"),
                    0.35D, 0, 1));
        } else {
            synth.add(new DropSpec("demon_core_fragment", 0.25D, 1, 2));
        }
        return List.copyOf(synth);
    }

    public static List<String> namedDropIds(String beastId) {
        if (beastId == null || beastId.isBlank()) {
            return List.of();
        }
        String key = beastId.trim().toLowerCase(Locale.ROOT);
        List<String> direct = SNAPSHOT.namedDrops().get(key);
        if (direct != null) {
            return direct;
        }
        return BeastBestiaryService.find(beastId)
                .map(BeastBestiaryService.BeastEntry::drops)
                .orElse(List.of());
    }

    /**
     * Roll loot for a killed ecology beast and grant to killer.
     * @return number of stacks granted
     */
    public static int grantKillLoot(ServerPlayer killer, String beastId, int tier, RandomSource random) {
        if (killer == null) {
            return 0;
        }
        RandomSource rng = random == null ? killer.getRandom() : random;
        int granted = 0;
        int t = BeastTierService.clampTier(tier);
        for (DropSpec drop : dropsForTier(t)) {
            if (rng.nextDouble() > drop.chance()) {
                continue;
            }
            int count = rollCount(rng, drop.minCount(), drop.maxCount());
            if (count <= 0) {
                continue;
            }
            Optional<ItemStack> stack = resolveStack(drop.itemId(), count);
            if (stack.isEmpty()) {
                continue;
            }
            if (!killer.getInventory().add(stack.get().copy())) {
                killer.drop(stack.get().copy(), false);
            }
            granted++;
        }
        // Named / bestiary explicit drops (lower chance each).
        for (String raw : namedDropIds(beastId)) {
            if (rng.nextDouble() > 0.55D) {
                continue;
            }
            Optional<ItemStack> stack = resolveStack(raw, 1);
            if (stack.isEmpty()) {
                continue;
            }
            if (!killer.getInventory().add(stack.get().copy())) {
                killer.drop(stack.get().copy(), false);
            }
            granted++;
        }
        // Tier common material table flavor.
        List<String> commons = SNAPSHOT.tierCommon().getOrDefault(t, List.of());
        if (!commons.isEmpty() && rng.nextDouble() < 0.5D) {
            String pick = commons.get(rng.nextInt(commons.size()));
            Optional<ItemStack> stack = resolveStack(pick, 1);
            if (stack.isPresent()) {
                if (!killer.getInventory().add(stack.get().copy())) {
                    killer.drop(stack.get().copy(), false);
                }
                granted++;
            }
        }
        return granted;
    }

    public static void handleEcologyKill(ServerPlayer killer, LivingEntity dead) {
        if (killer == null || dead == null) {
            return;
        }
        var tag = dead.getPersistentData();
        if (!tag.getBoolean("seeking_immortals_ecology_beast")
                && !tag.contains("seeking_immortals_beast_id")) {
            // Boss path still unlocks bestiary when boss_id present.
            if (tag.contains(com.xunxian.seekingimmortals.worldpack.BossEncounterService.BOSS_TAG)) {
                String bossId = com.xunxian.seekingimmortals.worldpack.BossEncounterService.bossIdOf(
                        dead instanceof net.minecraft.world.entity.Mob mob ? mob : null);
                if (!bossId.isBlank()) {
                    BestiaryUnlockService.unlock(killer, bossId, BestiaryUnlockService.UnlockKind.KILL);
                }
            }
            return;
        }
        String beastId = tag.getString("seeking_immortals_beast_id");
        if (beastId == null || beastId.isBlank()) {
            // Fall back to summon id ecology_X
            if (dead instanceof com.xunxian.seekingimmortals.entity.SummonedServitorEntity servitor) {
                String sid = servitor.getSummonId();
                if (sid != null && sid.startsWith("ecology_")) {
                    beastId = sid.substring("ecology_".length());
                } else if (sid != null && sid.startsWith("boss_")) {
                    beastId = sid.substring("boss_".length());
                }
            }
        }
        int tier = tag.contains("seeking_immortals_beast_tier")
                ? tag.getInt("seeking_immortals_beast_tier")
                : BeastBestiaryService.find(beastId).map(BeastBestiaryService.BeastEntry::tier).orElse(1);
        grantKillLoot(killer, beastId, tier, killer.getRandom());
        BestiaryUnlockService.unlock(killer, beastId, BestiaryUnlockService.UnlockKind.KILL);
    }

    private static int rollCount(RandomSource random, int min, int max) {
        int a = Math.min(min, max);
        int b = Math.max(min, max);
        if (b <= 0) {
            return 0;
        }
        if (a < 0) {
            a = 0;
        }
        if (a == b) {
            return a;
        }
        return a + random.nextInt(b - a + 1);
    }

    private static Snapshot load() {
        Map<String, List<DropSpec>> bands = new LinkedHashMap<>();
        Map<Integer, List<String>> common = new LinkedHashMap<>();
        Map<Integer, List<String>> uncommon = new LinkedHashMap<>();
        Map<Integer, List<String>> rare = new LinkedHashMap<>();
        Map<String, List<String>> named = new LinkedHashMap<>();

        JsonObject lootTiers = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_loot_tiers.json");
        if (lootTiers != null && lootTiers.has("tiers") && lootTiers.get("tiers").isJsonArray()) {
            for (JsonElement element : lootTiers.getAsJsonArray("tiers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String range = str(o, "tier_range");
                if (range.isBlank()) {
                    continue;
                }
                List<DropSpec> drops = new ArrayList<>();
                if (o.has("drops") && o.get("drops").isJsonArray()) {
                    for (JsonElement de : o.getAsJsonArray("drops")) {
                        if (!de.isJsonObject()) {
                            continue;
                        }
                        JsonObject d = de.getAsJsonObject();
                        String item = str(d, "item");
                        if (item.isBlank()) {
                            continue;
                        }
                        double chance = d.has("chance") ? d.get("chance").getAsDouble() : 0.25D;
                        int min = 1;
                        int max = 1;
                        if (d.has("count")) {
                            JsonElement c = d.get("count");
                            if (c.isJsonArray() && c.getAsJsonArray().size() >= 2) {
                                min = c.getAsJsonArray().get(0).getAsInt();
                                max = c.getAsJsonArray().get(1).getAsInt();
                            } else if (c.isJsonPrimitive()) {
                                min = max = c.getAsInt();
                            }
                        }
                        drops.add(new DropSpec(resolveItemId(item), chance, min, max));
                    }
                }
                bands.put(range, List.copyOf(drops));
            }
        }

        JsonObject materials = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_materials_loot_v92.json");
        if (materials != null) {
            if (materials.has("tier_loot_table") && materials.get("tier_loot_table").isJsonArray()) {
                for (JsonElement element : materials.getAsJsonArray("tier_loot_table")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = element.getAsJsonObject();
                    int tier = o.has("tier") ? o.get("tier").getAsInt() : 0;
                    if (tier < 1) {
                        continue;
                    }
                    common.put(tier, stringList(o, "common"));
                    uncommon.put(tier, stringList(o, "uncommon"));
                    rare.put(tier, stringList(o, "rare"));
                }
            }
            if (materials.has("named_beast_drops") && materials.get("named_beast_drops").isJsonArray()) {
                for (JsonElement element : materials.getAsJsonArray("named_beast_drops")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = element.getAsJsonObject();
                    String beast = str(o, "beast");
                    if (beast.isBlank()) {
                        continue;
                    }
                    // Resolve display → id when possible.
                    String id = BeastBestiaryService.find(beast).map(BeastBestiaryService.BeastEntry::id).orElse(beast.toLowerCase(Locale.ROOT));
                    named.put(id, stringList(o, "drops"));
                }
            }
            if (materials.has("named_creature_loot") && materials.get("named_creature_loot").isJsonArray()) {
                for (JsonElement element : materials.getAsJsonArray("named_creature_loot")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = element.getAsJsonObject();
                    String id = str(o, "id");
                    if (id.isBlank()) {
                        id = str(o, "beast");
                    }
                    if (id.isBlank()) {
                        continue;
                    }
                    named.putIfAbsent(id.toLowerCase(Locale.ROOT), stringList(o, "drops"));
                }
            }
        }

        return new Snapshot(
                Collections.unmodifiableMap(bands),
                Collections.unmodifiableMap(common),
                Collections.unmodifiableMap(uncommon),
                Collections.unmodifiableMap(rare),
                Collections.unmodifiableMap(named));
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> list = new ArrayList<>();
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return list;
        }
        for (JsonElement e : object.getAsJsonArray(key)) {
            if (e.isJsonPrimitive()) {
                String v = e.getAsString().trim();
                if (!v.isBlank()) {
                    list.add(v);
                }
            }
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BeastLootService.class.getClassLoader().getResourceAsStream(path)) {
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
