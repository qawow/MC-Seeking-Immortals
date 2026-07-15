package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Wave491: runtime consumer of text_material/spawn_tables.json.
 * Spawns SummonedServitor beast proxies weighted by region/biome; denser on leyline clusters.
 */
public final class BeastSpawnTableService {
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
            total += effectiveWeight(weight, cluster);
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int pick = random.nextInt(total);
        int cursor = 0;
        for (Weight weight : table.weights()) {
            cursor += effectiveWeight(weight, cluster);
            if (pick < cursor) {
                return Optional.of(weight);
            }
        }
        return Optional.of(table.weights().get(table.weights().size() - 1));
    }

    private static int effectiveWeight(Weight weight, boolean cluster) {
        int base = Math.max(1, weight.weight());
        if (!cluster) {
            return base;
        }
        // Cluster densifies mid/high tier beasts.
        if (weight.tier() >= 4) {
            return base * 3;
        }
        if (weight.tier() >= 2) {
            return base * 2;
        }
        return base;
    }

    /**
     * Spawn 1-N beast proxies near player from a matching table.
     */
    public static int spawnNearPlayer(ServerPlayer player, String regionHint, int count) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        BlockPos pos = player.blockPosition();
        String biomePath = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
        boolean cluster = SpiritualAuraManager.isLeylineCluster(level, new ChunkPos(pos));
        Optional<Table> table = findTable(regionHint, biomePath);
        if (table.isEmpty()) {
            table = findTable("tiannan", "forest");
        }
        if (table.isEmpty()) {
            return 0;
        }
        int want = Math.max(1, count + (cluster ? 2 : 0));
        int spawned = 0;
        RandomSource random = player.getRandom();
        for (int i = 0; i < want; i++) {
            Optional<Weight> roll = roll(table.get(), random, cluster);
            if (roll.isEmpty()) {
                continue;
            }
            Weight weight = roll.get();
            double health = 18.0D + weight.tier() * 6.0D;
            double damage = 3.0D + weight.tier() * 1.5D;
            int life = 20 * (40 + weight.tier() * 10);
            boolean ok = SummonHonestMvpService.spawnConfigured(
                    player,
                    "ecology_" + weight.beastId(),
                    life,
                    health,
                    damage,
                    SummonedServitorEntity.Archetype.BEAST);
            // spawnConfigured currently requires owner and may count against cap; use wild hostile shell path instead.
            if (!ok) {
                spawned += spawnWildProxy(level, player, weight, i);
            } else {
                spawned++;
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

    private static int spawnWildProxy(ServerLevel level, ServerPlayer player, Weight weight, int index) {
        SummonedServitorEntity entity = com.xunxian.seekingimmortals.registry.ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (entity == null) {
            return 0;
        }
        entity.moveTo(player.getX() + (index - 1) * 1.4D, player.getY(), player.getZ() + 1.8D + index * 0.3D,
                player.getYRot(), 0.0F);
        double health = 18.0D + weight.tier() * 6.0D;
        double damage = 3.0D + weight.tier() * 1.5D;
        entity.configureHostileTrial("ecology_" + weight.beastId(), 20 * (50 + weight.tier() * 12),
                health, damage, SummonedServitorEntity.Archetype.BEAST);
        entity.getPersistentData().putBoolean("seeking_immortals_ecology_beast", true);
        entity.getPersistentData().putString("seeking_immortals_beast_id", weight.beastId());
        entity.getPersistentData().putInt("seeking_immortals_beast_tier", weight.tier());
        level.addFreshEntity(entity);
        return 1;
    }

    private static List<Table> loadTables() {
        List<Table> tables = new ArrayList<>();
        String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/spawn_tables.json";
        try (InputStream stream = BeastSpawnTableService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return List.of();
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
                            weights.add(new Weight(beastId, Math.max(1, tier), Math.max(1, weight)));
                        }
                    }
                    if (!weights.isEmpty()) {
                        tables.add(new Table(region, biome, List.copyOf(weights)));
                    }
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.copyOf(tables);
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
