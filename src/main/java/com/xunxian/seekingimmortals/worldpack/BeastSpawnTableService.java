package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastCompanionService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * M10: runtime consumer of spawn_tables.json + region_spawn_tables_v98.
 * Spawns real data-driven beasts weighted by region/biome; denser on leyline clusters.
 * Red lines: true-spirit / companion beasts never enter daily tables; per-chunk cap + dedupe.
 */
public final class BeastSpawnTableService {
    /** Hard cap of ecology beasts near a player spawn request. */
    public static final int MAX_SPAWN_PER_REQUEST = 6;
    /** Hard cap of ecology beasts in a chunk radius. */
    public static final int MAX_ECOLOGY_NEAR = 12;
    public static final double NEAR_RADIUS = 48.0D;

    private static final List<Table> TABLES = loadTables();

    private BeastSpawnTableService() {}

    public record Weight(String beastId, int tier, int weight) {}

    public record Table(String region, String biome, List<Weight> weights) {}

    public static int tableCount() {
        return TABLES.size();
    }

    public static List<Table> tables() {
        return TABLES;
    }

    public static Optional<Table> findTable(String regionHint, String biomeHint) {
        String region = normalize(regionHint);
        String biome = normalize(biomeHint);
        Table best = null;
        int bestScore = -1;
        for (Table table : TABLES) {
            int score = 0;
            if (!region.isBlank() && region.equals(table.region())) {
                score += 3;
            } else if (!region.isBlank() && (region.contains(table.region()) || table.region().contains(region))) {
                score += 2;
            }
            if (!biome.isBlank() && (biome.contains(table.biome()) || table.biome().contains(biome)
                    || "any".equals(table.biome()) || table.biome().isBlank())) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = table;
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<Weight> roll(Table table, RandomSource random, boolean cluster) {
        if (table == null || table.weights().isEmpty()) {
            return Optional.empty();
        }
        int total = 0;
        for (Weight weight : table.weights()) {
            if (isBanned(weight.beastId())) {
                continue;
            }
            total += effectiveWeight(weight, cluster);
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int pick = random.nextInt(total);
        int cursor = 0;
        for (Weight weight : table.weights()) {
            if (isBanned(weight.beastId())) {
                continue;
            }
            cursor += effectiveWeight(weight, cluster);
            if (pick < cursor) {
                return Optional.of(weight);
            }
        }
        // Last non-banned.
        for (int i = table.weights().size() - 1; i >= 0; i--) {
            Weight w = table.weights().get(i);
            if (!isBanned(w.beastId())) {
                return Optional.of(w);
            }
        }
        return Optional.empty();
    }

    public static Optional<Weight> rollFor(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level == null || pos == null || random == null) {
            return Optional.empty();
        }
        String region = com.xunxian.seekingimmortals.region.RegionRegistry.resolveRegionId(level, pos);
        String biome = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
        boolean cluster = SpiritualAuraManager.isLeylineCluster(level, new ChunkPos(pos));
        Optional<Table> table = findTable(region, biome);
        if (table.isEmpty()) {
            table = findTable("tiannan", "forest");
        }
        return table.flatMap(value -> roll(value, random, cluster));
    }

    public static boolean isBanned(String beastId) {
        if (beastId == null || beastId.isBlank()) {
            return true;
        }
        if (BeastCompanionService.isProtectedCompanion(beastId)) {
            return true;
        }
        return BeastBestiaryService.isBannedFromDailySpawn(beastId);
    }

    private static int effectiveWeight(Weight weight, boolean cluster) {
        int base = Math.max(1, weight.weight());
        if (!cluster) {
            return base;
        }
        if (weight.tier() >= 4) {
            return base * 3;
        }
        if (weight.tier() >= 2) {
            return base * 2;
        }
        return base;
    }

    /**
     * Spawn 1-N wild beasts near player from a matching table.
     */
    public static int spawnNearPlayer(ServerPlayer player, String regionHint, int count) {
        return spawnNearPlayer(player, regionHint, count, true, null);
    }

    /** Spawns at most {@code count}; daily encounter plans use this exact upper bound. */
    public static int spawnNearPlayerExact(ServerPlayer player, String regionHint, int count) {
        return spawnNearPlayer(player, regionHint, count, false, null);
    }

    /** Exact daily-event spawn with a pre-insertion authority binder. */
    public static int spawnNearPlayerExact(ServerPlayer player, String regionHint, int count,
                                           Consumer<CultivationBeastEntity> binder) {
        return spawnNearPlayer(player, regionHint, count, false, binder);
    }

    private static int spawnNearPlayer(ServerPlayer player, String regionHint, int count,
                                       boolean allowLeylineBonus, Consumer<CultivationBeastEntity> binder) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0;
        }
        BlockPos pos = player.blockPosition();
        String biomePath = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
        boolean cluster = SpiritualAuraManager.isLeylineCluster(level, new ChunkPos(pos));
        Optional<Table> table = findTable(regionHint, biomePath);
        if (table.isEmpty()) {
            // Prefer M06 region id if available.
            try {
                String resolved = com.xunxian.seekingimmortals.region.RegionRegistry.resolveRegionId(level, pos, regionHint);
                if (resolved != null && !resolved.isBlank()) {
                    table = findTable(resolved, biomePath);
                }
            } catch (Throwable ignored) {
                // RegionRegistry may be absent in pure unit tests.
            }
        }
        if (table.isEmpty()) {
            table = findTable("tiannan", "forest");
        }
        if (table.isEmpty()) {
            return 0;
        }
        int existing = countEcologyNear(level, player.getX(), player.getY(), player.getZ());
        int room = Math.max(0, MAX_ECOLOGY_NEAR - existing);
        if (room <= 0) {
            return 0;
        }
        int want = spawnRequestLimit(count, cluster, allowLeylineBonus, room);
        int spawned = 0;
        RandomSource random = player.getRandom();
        // Dedupe beast ids within this request.
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (int i = 0; i < want * 3 && spawned < want; i++) {
            Optional<Weight> roll = roll(table.get(), random, cluster);
            if (roll.isEmpty()) {
                continue;
            }
            Weight weight = roll.get();
            if (isBanned(weight.beastId())) {
                continue;
            }
            int prior = seen.getOrDefault(weight.beastId(), 0);
            // Soft dedupe: same id at most 2 per request unless table is tiny.
            if (prior >= 2 && table.get().weights().size() > 2) {
                continue;
            }
            if (spawnWildBeast(level, player, weight, spawned, random, binder)) {
                spawned++;
                seen.put(weight.beastId(), prior + 1);
            }
        }
        if (spawned > 0) {
            player.displayClientMessage(Component.translatable(
                    cluster ? "message.seeking_immortals.spawn_table.cluster"
                            : "message.seeking_immortals.spawn_table.spawn",
                    table.get().region(), spawned), true);
        }
        return spawned;
    }

    static int spawnRequestLimit(int count, boolean cluster, boolean allowLeylineBonus, int room) {
        int requested = allowLeylineBonus
                ? Math.max(1, count + (cluster ? 2 : 0))
                : Math.max(0, count);
        return Math.min(MAX_SPAWN_PER_REQUEST, Math.min(Math.max(0, room), requested));
    }

    public static int countEcologyNear(ServerLevel level, double x, double y, double z) {
        if (level == null) {
            return 0;
        }
        AABB box = new AABB(x - NEAR_RADIUS, y - 16.0D, z - NEAR_RADIUS,
                x + NEAR_RADIUS, y + 16.0D, z + NEAR_RADIUS);
        int count = 0;
        for (Entity entity : level.getEntities(null, box)) {
            if (entity.getPersistentData().getBoolean("seeking_immortals_ecology_beast")) {
                count++;
            }
        }
        return count;
    }

    private static boolean spawnWildBeast(ServerLevel level, ServerPlayer player, Weight weight,
                                          int index, RandomSource random,
                                          Consumer<CultivationBeastEntity> binder) {
        CultivationBeastEntity entity = ModEntities.CULTIVATION_BEAST.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.configureWild(weight.beastId(), weight.tier());
        BlockPos spawnPos = findSpawnPosition(
                level, player.blockPosition(), random, index, entity.getBodyPlan());
        entity.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(entity)) {
            entity.moveTo(entity.getX(), entity.getY() + 1.0D, entity.getZ(), entity.getYRot(), 0.0F);
        }
        if (binder != null) {
            binder.accept(entity);
        }
        if (!level.noCollision(entity) || !level.addFreshEntity(entity)) {
            return false;
        }
        if (!player.isCreative() && !player.isSpectator()) {
            entity.setTarget(player);
        }
        return true;
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos origin, RandomSource random, int index,
                                              CultivationBeastEntity.BodyPlan bodyPlan) {
        if (bodyPlan == CultivationBeastEntity.BodyPlan.AQUATIC) {
            Optional<BlockPos> water = findWaterSpawnPosition(level, origin, random, index);
            if (water.isPresent()) {
                return water.get();
            }
        }
        return findAmphibiousLandSpawnPosition(level, origin, random, index);
    }

    private static Optional<BlockPos> findWaterSpawnPosition(ServerLevel level, BlockPos origin,
                                                              RandomSource random, int index) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = 9 + random.nextInt(10) + index;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos horizontal = new BlockPos(x, origin.getY(), z);
            for (int offset = 7; offset >= -9; offset--) {
                BlockPos water = horizontal.offset(0, offset, 0);
                if (level.getFluidState(water).is(FluidTags.WATER)
                        && level.getFluidState(water.above()).is(FluidTags.WATER)) {
                    return Optional.of(water);
                }
            }
        }
        return Optional.empty();
    }

    private static BlockPos findAmphibiousLandSpawnPosition(ServerLevel level, BlockPos origin,
                                                             RandomSource random, int index) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = 9 + random.nextInt(10) + index;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos horizontal = new BlockPos(x, origin.getY(), z);
            for (int offset = 5; offset >= -7; offset--) {
                BlockPos feet = horizontal.offset(0, offset, 0);
                if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                    return feet;
                }
            }
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, horizontal);
            if (Math.abs(surface.getY() - origin.getY()) <= 16) {
                return surface;
            }
        }
        return origin.offset(3 + index, 0, 3);
    }

    private static List<Table> loadTables() {
        List<Table> tables = new ArrayList<>();
        // Base spawn_tables.json
        tables.addAll(loadLegacySpawnTables());
        // M10 region_spawn_tables_v98 keyed by M06 region_id
        tables.addAll(loadRegionSpawnTables());
        // Filter banned weights out of every table (red line).
        List<Table> cleaned = new ArrayList<>();
        for (Table table : tables) {
            List<Weight> weights = new ArrayList<>();
            for (Weight w : table.weights()) {
                if (!isBanned(w.beastId())) {
                    weights.add(w);
                }
            }
            if (!weights.isEmpty()) {
                cleaned.add(new Table(table.region(), table.biome(), List.copyOf(weights)));
            }
        }
        return List.copyOf(cleaned);
    }

    private static List<Table> loadLegacySpawnTables() {
        List<Table> tables = new ArrayList<>();
        String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/spawn_tables.json";
        try (InputStream stream = BeastSpawnTableService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return tables;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray array = root.has("tables") && root.get("tables").isJsonArray()
                        ? root.getAsJsonArray("tables") : new JsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject object = element.getAsJsonObject();
                    String region = str(object, "region");
                    String biome = str(object, "biome");
                    List<Weight> weights = new ArrayList<>();
                    if (object.has("weights") && object.get("weights").isJsonArray()) {
                        for (JsonElement weightElement : object.getAsJsonArray("weights")) {
                            if (!weightElement.isJsonObject()) {
                                continue;
                            }
                            JsonObject weightObject = weightElement.getAsJsonObject();
                            String beastId = str(weightObject, "beast_id");
                            if (beastId.isBlank()) {
                                continue;
                            }
                            int tier = weightObject.has("tier") ? weightObject.get("tier").getAsInt() : 1;
                            int weight = weightObject.has("weight") ? weightObject.get("weight").getAsInt() : 1;
                            if (weight > 0) {
                                weights.add(new Weight(beastId, Math.max(1, tier), weight));
                            }
                        }
                    }
                    if (!weights.isEmpty()) {
                        tables.add(new Table(region, biome, List.copyOf(weights)));
                    }
                }
            }
        } catch (Exception ignored) {
            return tables;
        }
        return tables;
    }

    private static List<Table> loadRegionSpawnTables() {
        List<Table> tables = new ArrayList<>();
        String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/region_spawn_tables_v98.json";
        try (InputStream stream = BeastSpawnTableService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return tables;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray regions = root.has("regions") && root.get("regions").isJsonArray()
                        ? root.getAsJsonArray("regions") : new JsonArray();
                for (JsonElement element : regions) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject regionObj = element.getAsJsonObject();
                    String regionId = str(regionObj, "id");
                    if (regionId.isBlank()) {
                        continue;
                    }
                    List<Weight> weights = new ArrayList<>();
                    if (regionObj.has("spawns") && regionObj.get("spawns").isJsonArray()) {
                        for (JsonElement spawnEl : regionObj.getAsJsonArray("spawns")) {
                            if (!spawnEl.isJsonObject()) {
                                continue;
                            }
                            JsonObject spawn = spawnEl.getAsJsonObject();
                            String beastId = str(spawn, "id");
                            if (beastId.isBlank()) {
                                continue;
                            }
                            int weight = spawn.has("weight") ? spawn.get("weight").getAsInt() : 10;
                            if (weight <= 0) {
                                continue;
                            }
                            int tier = parseTier(spawn);
                            weights.add(new Weight(beastId, BeastTierService.clampTier(tier), weight));
                        }
                    }
                    if (!weights.isEmpty()) {
                        // Region tables are biome-agnostic ("any").
                        tables.add(new Table(regionId, "any", List.copyOf(weights)));
                    }
                }
            }
        } catch (Exception ignored) {
            return tables;
        }
        return tables;
    }

    private static int parseTier(JsonObject spawn) {
        if (spawn.has("tier")) {
            JsonElement t = spawn.get("tier");
            if (t.isJsonPrimitive()) {
                try {
                    return t.getAsInt();
                } catch (Exception ignored) {
                    // fall through
                }
            } else if (t.isJsonArray() && t.getAsJsonArray().size() > 0) {
                try {
                    return t.getAsJsonArray().get(0).getAsInt();
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        // Fall back to bestiary tier.
        return BeastBestiaryService.find(str(spawn, "id"))
                .map(BeastBestiaryService.BeastEntry::tier)
                .orElse(1);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString().trim().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
