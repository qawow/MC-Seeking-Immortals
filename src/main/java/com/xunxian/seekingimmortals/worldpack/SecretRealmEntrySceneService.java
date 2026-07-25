package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/** Deterministic entry landmarks that give each dedicated realm a readable identity. */
public final class SecretRealmEntrySceneService {
    public enum Profile {
        ANCIENT_RUIN,
        BLOOD_GROVE,
        ABYSS_RIFT,
        FROST_TEMPLE,
        CATACOMB,
        PUPPET_TOWER,
        SPIRIT_GARDEN,
        ASURA_ARENA,
        CELESTIAL_COURT,
        CAVE_TRIAL
    }

    private static final Map<String, Profile> PROFILES = Map.ofEntries(
            Map.entry("ancient_cultivator_ruins", Profile.ANCIENT_RUIN),
            Map.entry("wild_ancient_ruins", Profile.ANCIENT_RUIN),
            Map.entry("blood_forbidden", Profile.BLOOD_GROVE),
            Map.entry("fallen_demon_depths", Profile.BLOOD_GROVE),
            Map.entry("fallen_demon_valley", Profile.BLOOD_GROVE),
            Map.entry("demon_gold_mountain", Profile.BLOOD_GROVE),
            Map.entry("chaotic_sea_abyss_rift", Profile.ABYSS_RIFT),
            Map.entry("void_palace", Profile.ABYSS_RIFT),
            Map.entry("diyuan", Profile.ABYSS_RIFT),
            Map.entry("guanghan_realm", Profile.FROST_TEMPLE),
            Map.entry("kunwu_mountain", Profile.FROST_TEMPLE),
            Map.entry("nether_river_land", Profile.CATACOMB),
            Map.entry("wild_ancient_tomb", Profile.CATACOMB),
            Map.entry("yinyang_ku", Profile.CATACOMB),
            Map.entry("yin_mountain_catacomb", Profile.CATACOMB),
            Map.entry("thousand_bamboo_puppet_tower", Profile.PUPPET_TOWER),
            Map.entry("spirit_grass_valley", Profile.SPIRIT_GARDEN),
            Map.entry("jiuxian_seclusion", Profile.SPIRIT_GARDEN),
            Map.entry("minor_asura_realm", Profile.ASURA_ARENA),
            Map.entry("asura_realm", Profile.ASURA_ARENA),
            Map.entry("immortal_realm", Profile.CELESTIAL_COURT),
            Map.entry("tianlan_secret_grotto", Profile.CAVE_TRIAL),
            Map.entry("seven_meridian_cave", Profile.CAVE_TRIAL),
            Map.entry("mist_cave_trial", Profile.CAVE_TRIAL));

    private SecretRealmEntrySceneService() {}

    public static Profile profileFor(String realmId) {
        return PROFILES.getOrDefault(normalize(realmId), Profile.ANCIENT_RUIN);
    }

    public static int profileCount() {
        return PROFILES.size();
    }

    public static void ensure(ServerLevel level, String realmId, BlockPos base) {
        if (level == null || base == null) {
            return;
        }
        String key = level.dimension().location() + "/" + normalize(realmId);
        SecretRealmSceneSavedData ledger = SecretRealmSceneSavedData.get(level.getServer());
        if (!ledger.isGenerated(key)) {
            generate(level, base, profileFor(realmId));
            ledger.markGenerated(key);
        }
        ensureLanding(level, base);
    }

    private static void generate(ServerLevel level, BlockPos base, Profile profile) {
        BlockState floor = floor(profile).defaultBlockState();
        BlockState accent = accent(profile).defaultBlockState();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                int distance = x * x + z * z;
                if (distance <= 64 && (Math.abs(x) == 8 || Math.abs(z) == 8 || distance <= 36)) {
                    level.setBlock(base.offset(x, 0, z), floor, 3);
                }
            }
        }
        ring(level, base, accent, 7);
        switch (profile) {
            case ANCIENT_RUIN -> ruinColumns(level, base, accent, 3);
            case BLOOD_GROVE -> grove(level, base, accent);
            case ABYSS_RIFT -> rift(level, base, accent);
            case FROST_TEMPLE -> temple(level, base, accent);
            case CATACOMB -> catacomb(level, base, accent);
            case PUPPET_TOWER -> tower(level, base, accent);
            case SPIRIT_GARDEN -> garden(level, base, accent);
            case ASURA_ARENA -> arena(level, base, accent);
            case CELESTIAL_COURT -> court(level, base, accent);
            case CAVE_TRIAL -> caveGate(level, base, accent);
        }
    }

    private static void ensureLanding(ServerLevel level, BlockPos base) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(base.offset(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 2, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(base.offset(0, 0, -2), ModBlocks.TELEPORT_ARRAY_PEDESTAL.get().defaultBlockState(), 3);
    }

    private static void ring(ServerLevel level, BlockPos base, BlockState state, int radius) {
        for (int x = -radius; x <= radius; x++) {
            level.setBlock(base.offset(x, 1, -radius), state, 3);
            level.setBlock(base.offset(x, 1, radius), state, 3);
        }
        for (int z = -radius + 1; z < radius; z++) {
            level.setBlock(base.offset(-radius, 1, z), state, 3);
            level.setBlock(base.offset(radius, 1, z), state, 3);
        }
    }

    private static void ruinColumns(ServerLevel level, BlockPos base, BlockState state, int height) {
        for (int x : new int[]{-5, 5}) {
            for (int z : new int[]{-5, 5}) {
                column(level, base.offset(x, 1, z), state, height + Math.floorMod(x + z, 2));
            }
        }
    }

    private static void grove(ServerLevel level, BlockPos base, BlockState state) {
        ruinColumns(level, base, state, 4);
        level.setBlock(base.offset(0, 1, 5), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, -5), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
    }

    private static void rift(ServerLevel level, BlockPos base, BlockState state) {
        for (int z = -5; z <= 5; z++) {
            level.setBlock(base.offset(-3, 1, z), state, 3);
            level.setBlock(base.offset(3, 1, z), state, 3);
        }
        column(level, base.offset(-3, 1, 0), state, 5);
        column(level, base.offset(3, 1, 0), state, 5);
    }

    private static void temple(ServerLevel level, BlockPos base, BlockState state) {
        for (int x = -5; x <= 5; x++) {
            level.setBlock(base.offset(x, 1, 5), state, 3);
        }
        column(level, base.offset(-5, 1, 5), state, 5);
        column(level, base.offset(5, 1, 5), state, 5);
        level.setBlock(base.offset(0, 1, 5), Blocks.BLUE_ICE.defaultBlockState(), 3);
    }

    private static void catacomb(ServerLevel level, BlockPos base, BlockState state) {
        for (int z = 2; z <= 7; z++) {
            level.setBlock(base.offset(-3, 1, z), state, 3);
            level.setBlock(base.offset(3, 1, z), state, 3);
        }
        caveGate(level, base.offset(0, 0, 4), state);
    }

    private static void tower(ServerLevel level, BlockPos base, BlockState state) {
        for (int y = 1; y <= 8; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) == 2) {
                        level.setBlock(base.offset(x, y, z + 5), state, 3);
                    }
                }
            }
        }
    }

    private static void garden(ServerLevel level, BlockPos base, BlockState state) {
        for (int x = -5; x <= 5; x += 2) {
            level.setBlock(base.offset(x, 1, 5), Blocks.MOSS_BLOCK.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 2, 5), state, 3);
        }
        level.setBlock(base.offset(0, 1, 5), Blocks.WATER.defaultBlockState(), 3);
    }

    private static void arena(ServerLevel level, BlockPos base, BlockState state) {
        for (int angle = 0; angle < 8; angle++) {
            int x = (int) Math.round(Math.cos(angle * Math.PI / 4.0D) * 6.0D);
            int z = (int) Math.round(Math.sin(angle * Math.PI / 4.0D) * 6.0D);
            column(level, base.offset(x, 1, z), state, 3);
        }
    }

    private static void court(ServerLevel level, BlockPos base, BlockState state) {
        caveGate(level, base.offset(0, 0, 4), state);
        ruinColumns(level, base, Blocks.QUARTZ_PILLAR.defaultBlockState(), 6);
    }

    private static void caveGate(ServerLevel level, BlockPos base, BlockState state) {
        column(level, base.offset(-3, 1, 5), state, 5);
        column(level, base.offset(3, 1, 5), state, 5);
        for (int x = -3; x <= 3; x++) {
            level.setBlock(base.offset(x, 5, 5), state, 3);
        }
    }

    private static void column(ServerLevel level, BlockPos start, BlockState state, int height) {
        for (int y = 0; y < height; y++) {
            level.setBlock(start.above(y), state, 3);
        }
    }

    private static Block floor(Profile profile) {
        return switch (profile) {
            case BLOOD_GROVE, ASURA_ARENA -> Blocks.POLISHED_BLACKSTONE;
            case ABYSS_RIFT -> Blocks.DEEPSLATE_TILES;
            case FROST_TEMPLE -> Blocks.PACKED_ICE;
            case CATACOMB, CAVE_TRIAL -> Blocks.COBBLED_DEEPSLATE;
            case PUPPET_TOWER -> Blocks.DARK_OAK_PLANKS;
            case SPIRIT_GARDEN -> Blocks.MOSSY_STONE_BRICKS;
            case CELESTIAL_COURT -> Blocks.QUARTZ_BLOCK;
            case ANCIENT_RUIN -> Blocks.STONE_BRICKS;
        };
    }

    private static Block accent(Profile profile) {
        return switch (profile) {
            case BLOOD_GROVE, ASURA_ARENA -> Blocks.CRIMSON_HYPHAE;
            case ABYSS_RIFT -> Blocks.CRYING_OBSIDIAN;
            case FROST_TEMPLE -> Blocks.BLUE_ICE;
            case CATACOMB, CAVE_TRIAL -> Blocks.POLISHED_DEEPSLATE;
            case PUPPET_TOWER -> Blocks.STRIPPED_DARK_OAK_WOOD;
            case SPIRIT_GARDEN -> Blocks.MOSS_BLOCK;
            case CELESTIAL_COURT -> Blocks.QUARTZ_PILLAR;
            case ANCIENT_RUIN -> ModBlocks.SPIRIT_ORE.get();
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
