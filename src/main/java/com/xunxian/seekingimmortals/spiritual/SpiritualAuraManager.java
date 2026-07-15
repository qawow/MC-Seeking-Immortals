package com.xunxian.seekingimmortals.spiritual;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class SpiritualAuraManager {
    public static final int BASE_AURA = 100;
    public static final int FORMATION_RADIUS = 16;
    private static final int FORMATION_VERTICAL_RANGE = 6;
    private static final int FORMATION_BONUS_PER_BLOCK = 50;
    private static final int MAX_FORMATION_COUNT = 4;
    private static final int LEYLINE_SEARCH_RADIUS_CHUNKS = 16;
    private static final ResourceLocation NETHER_DIMENSION = new ResourceLocation("minecraft", "the_nether");
    private static final ResourceLocation END_DIMENSION = new ResourceLocation("minecraft", "the_end");
    private static final ResourceLocation TIANYUAN_DIMENSION = new ResourceLocation("seeking_immortals", "tianyuan");
    private static final ResourceLocation SPIRIT_FENGYUAN_DIMENSION = new ResourceLocation("seeking_immortals", "spirit_fengyuan");
    private static final ResourceLocation YIN_MING_POCKET_DIMENSION = new ResourceLocation("seeking_immortals", "yin_ming_pocket");
    private static final ResourceLocation NETHER_RIVER_POCKET_DIMENSION = new ResourceLocation("seeking_immortals", "nether_river_pocket");
    private static final ResourceLocation DEMON_RIFT_DIMENSION = new ResourceLocation("seeking_immortals", "demon_rift");

    private SpiritualAuraManager() {}

    public static AuraInfo getAuraInfo(Level level, BlockPos pos) {
        double dimensionMultiplier = getDimensionMultiplier(level);
        double biomeMultiplier = getBiomeMultiplier(level, pos);
        ChunkPos chunkPos = new ChunkPos(pos);
        double leylineMultiplier = getLeylineMultiplier(level, chunkPos);
        int formationBonus = getFormationBonus(level, pos);
        AuraNature nature = getAuraNature(level);
        boolean cluster = isLeylineCluster(level, chunkPos);
        int concentration = Math.max(1, (int)Math.round(BASE_AURA * dimensionMultiplier * biomeMultiplier * leylineMultiplier + formationBonus));
        if (cluster) {
            concentration += 25;
        }
        return new AuraInfo(concentration, dimensionMultiplier, biomeMultiplier, leylineMultiplier, formationBonus, nature, leylineMultiplier >= 3.0D, cluster);
    }

    public static int adjustSpiritualPowerGain(int baseGain, AuraInfo auraInfo) {
        return Math.max(1, (int)Math.round(baseGain * auraInfo.concentration() / (double)BASE_AURA));
    }

    public static int adjustCultivationExpGain(int baseGain, AuraInfo auraInfo) {
        double multiplier = Math.sqrt(auraInfo.concentration() / (double)BASE_AURA);
        return Math.max(1, (int)Math.round(baseGain * multiplier));
    }

    public static Optional<LeylineTarget> findNearestLeyline(ServerLevel level, BlockPos origin) {
        ChunkPos originChunk = new ChunkPos(origin);
        LeylineTarget best = null;
        double bestDistanceSqr = Double.MAX_VALUE;
        for (int dx = -LEYLINE_SEARCH_RADIUS_CHUNKS; dx <= LEYLINE_SEARCH_RADIUS_CHUNKS; dx++) {
            for (int dz = -LEYLINE_SEARCH_RADIUS_CHUNKS; dz <= LEYLINE_SEARCH_RADIUS_CHUNKS; dz++) {
                int chunkX = originChunk.x + dx;
                int chunkZ = originChunk.z + dz;
                double multiplier = getLeylineCoreMultiplier(level, chunkX, chunkZ);
                if (multiplier < 3.0D) continue;
                int blockX = chunkX * 16 + 8;
                int blockZ = chunkZ * 16 + 8;
                double distanceSqr = origin.distToCenterSqr(blockX, origin.getY(), blockZ);
                if (distanceSqr < bestDistanceSqr) {
                    bestDistanceSqr = distanceSqr;
                    best = new LeylineTarget(blockX, blockZ, multiplier, Math.sqrt(distanceSqr));
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static double getLeylineMultiplier(Level level, ChunkPos chunkPos) {
        double best = 1.0D;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double core = getLeylineCoreMultiplier(level, chunkPos.x + dx, chunkPos.z + dz);
                if (core <= 1.0D) continue;
                double penalty = dx == 0 && dz == 0 ? 0.0D : 0.5D;
                best = Math.max(best, Math.max(1.0D, core - penalty));
            }
        }
        return best;
    }

    public static double getLeylineCoreMultiplier(Level level, int chunkX, int chunkZ) {
        long seed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        return getLeylineCoreMultiplier(seed, chunkX, chunkZ);
    }

    /**
     * Wave493: seed-only leyline core for worldgen structure start decisions.
     * Same thresholds as live aura authority.
     */
    public static double getLeylineCoreMultiplier(long seed, int chunkX, int chunkZ) {
        long hash = mix(seed, chunkX, chunkZ);
        int value = (int) Math.floorMod(hash, 10_000L);
        double core;
        if (value < 50) core = 5.0D;
        else if (value < 100) core = 4.0D;
        else if (value < 200) core = 3.0D;
        else core = 1.0D;
        if (core >= 3.0D) {
            int cluster = countNearbyMajorVeins(seed, chunkX, chunkZ, 2);
            if (cluster >= 3) {
                core = Math.min(6.5D, core + 0.75D + (cluster - 3) * 0.15D);
            } else if (cluster >= 2) {
                core = Math.min(6.0D, core + 0.35D);
            }
        }
        return core;
    }

    /** True when this chunk is a major leyline core (x3+) under hash authority. */
    public static boolean isMajorLeylineChunk(long seed, int chunkX, int chunkZ) {
        long hash = mix(seed, chunkX, chunkZ);
        int value = (int) Math.floorMod(hash, 10_000L);
        return value < 200;
    }

    /**
     * 1 = x3 core, 2 = x4 core, 3 = x5 core (pre-cluster), 0 = none.
     */
    public static int majorLeylineTier(long seed, int chunkX, int chunkZ) {
        long hash = mix(seed, chunkX, chunkZ);
        int value = (int) Math.floorMod(hash, 10_000L);
        if (value < 50) return 3;
        if (value < 100) return 2;
        if (value < 200) return 1;
        return 0;
    }

    /**
     * Wave489: count major leyline cores (x3+) in a square radius of chunks (excluding center).
     * Used for cluster density and ecology hooks.
     */
    public static int countNearbyMajorVeins(Level level, int chunkX, int chunkZ, int radius) {
        long seed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        return countNearbyMajorVeins(seed, chunkX, chunkZ, radius);
    }

    public static int countNearbyMajorVeins(long seed, int chunkX, int chunkZ, int radius) {
        int r = Math.max(1, radius);
        int count = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (isMajorLeylineChunk(seed, chunkX + dx, chunkZ + dz)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** True when the chunk sits inside a dense multi-vein cluster. */
    public static boolean isLeylineCluster(Level level, ChunkPos chunkPos) {
        if (chunkPos == null || level == null) {
            return false;
        }
        return getLeylineCoreMultiplier(level, chunkPos.x, chunkPos.z) >= 3.0D
                && countNearbyMajorVeins(level, chunkPos.x, chunkPos.z, 2) >= 2;
    }

    private static double getDimensionMultiplier(Level level) {
        return getDimensionMultiplier(level.dimension().location());
    }

    static double getDimensionMultiplier(ResourceLocation dimensionId) {
        if (NETHER_DIMENSION.equals(dimensionId)) return 1.2D;
        if (END_DIMENSION.equals(dimensionId)) return 1.5D;
        if (TIANYUAN_DIMENSION.equals(dimensionId)) return 2.0D;
        if (SPIRIT_FENGYUAN_DIMENSION.equals(dimensionId)) return 1.8D;
        if (YIN_MING_POCKET_DIMENSION.equals(dimensionId)) return 0.7D;
        if (NETHER_RIVER_POCKET_DIMENSION.equals(dimensionId)) return 0.85D;
        if (DEMON_RIFT_DIMENSION.equals(dimensionId)) return 1.1D;
        String path = dimensionId.getPath();
        if (path.contains("yin")) return 0.7D;
        if (path.contains("nether_river")) return 0.85D;
        if (path.contains("secret") || path.contains("mystic") || path.contains("immortal_mansion") || path.contains("xianfu")) {
            return 10.0D;
        }
        return 1.0D;
    }

    private static AuraNature getAuraNature(Level level) {
        return getAuraNature(level.dimension().location());
    }

    static AuraNature getAuraNature(ResourceLocation dimensionId) {
        if (NETHER_DIMENSION.equals(dimensionId)) return AuraNature.BODY_REFINING_FIRE_DEMONIC;
        if (END_DIMENSION.equals(dimensionId)) return AuraNature.LAW_VOID;
        if (TIANYUAN_DIMENSION.equals(dimensionId) || SPIRIT_FENGYUAN_DIMENSION.equals(dimensionId)) {
            return AuraNature.SPIRIT_REALM;
        }
        if (YIN_MING_POCKET_DIMENSION.equals(dimensionId) || NETHER_RIVER_POCKET_DIMENSION.equals(dimensionId)) {
            return AuraNature.YIN_UNDERWORLD;
        }
        if (DEMON_RIFT_DIMENSION.equals(dimensionId)) {
            return AuraNature.BODY_REFINING_FIRE_DEMONIC;
        }
        String path = dimensionId.getPath();
        if (path.contains("yin") || path.contains("nether_river")) {
            return AuraNature.YIN_UNDERWORLD;
        }
        if (path.contains("secret") || path.contains("mystic") || path.contains("immortal_mansion") || path.contains("xianfu")) {
            return AuraNature.SECRET_REALM;
        }
        return AuraNature.NORMAL;
    }

    private static double getBiomeMultiplier(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (isDeepSeaIsland(level, pos, biome)) return 2.0D;
        if (biome.is(BiomeTags.IS_MOUNTAIN)) return 1.5D;
        return 1.0D;
    }

    private static boolean isDeepSeaIsland(Level level, BlockPos pos, Holder<Biome> currentBiome) {
        if (currentBiome.is(BiomeTags.IS_OCEAN)) return false;
        int[][] offsets = new int[][] {
                {64, 0}, {-64, 0}, {0, 64}, {0, -64},
                {48, 48}, {-48, 48}, {48, -48}, {-48, -48}
        };
        for (int[] offset : offsets) {
            Holder<Biome> sample = level.getBiome(pos.offset(offset[0], 0, offset[1]));
            if (sample.is(BiomeTags.IS_DEEP_OCEAN)) return true;
        }
        return false;
    }

    private static int getFormationBonus(Level level, BlockPos pos) {
        int count = 0;
        BlockPos from = pos.offset(-FORMATION_RADIUS, -FORMATION_VERTICAL_RANGE, -FORMATION_RADIUS);
        BlockPos to = pos.offset(FORMATION_RADIUS, FORMATION_VERTICAL_RANGE, FORMATION_RADIUS);
        for (BlockPos scanPos : BlockPos.betweenClosed(from, to)) {
            BlockState state = level.getBlockState(scanPos);
            if (state.is(ModBlocks.SPIRIT_GATHERING_ARRAY.get())) {
                count++;
                if (count >= MAX_FORMATION_COUNT) break;
            }
        }
        return count * FORMATION_BONUS_PER_BLOCK;
    }

    private static long mix(long seed, int chunkX, int chunkZ) {
        long value = seed;
        value ^= (long)chunkX * 341873128712L;
        value ^= (long)chunkZ * 132897987541L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value & Long.MAX_VALUE;
    }

    public record AuraInfo(int concentration, double dimensionMultiplier, double biomeMultiplier, double leylineMultiplier,
                           int formationBonus, AuraNature nature, boolean leyline, boolean cluster) {}

    public record LeylineTarget(int blockX, int blockZ, double multiplier, double distance) {}

    public enum AuraNature {
        NORMAL("天地灵气"),
        BODY_REFINING_FIRE_DEMONIC("火煞魔气/炼体灵气"),
        LAW_VOID("虚空法则灵气"),
        SPIRIT_REALM("灵界浓郁灵气"),
        YIN_UNDERWORLD("阴冥寒煞灵气"),
        SECRET_REALM("秘境仙府灵气");

        private final String displayName;

        AuraNature(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
