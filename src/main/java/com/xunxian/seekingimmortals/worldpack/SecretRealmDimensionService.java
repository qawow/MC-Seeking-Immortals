package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Independent secret-realm dimension pack.
 * Wave47: blood/void/mist/fallen_demon.
 * Wave49: asura_realm + immortal_realm dedicated dims.
 */
public final class SecretRealmDimensionService {
    private static final Map<String, String> REALM_DIMENSIONS = buildMap();

    private static Map<String, String> buildMap() {
        Map<String, String> map = new HashMap<>();
        map.put("blood_forbidden", SeekingImmortalsMod.MODID + ":secret_realm_blood_forbidden");
        map.put("void_palace", SeekingImmortalsMod.MODID + ":secret_realm_void_palace");
        map.put("mist_cave_trial", SeekingImmortalsMod.MODID + ":secret_realm_mist_cave");
        map.put("fallen_demon_valley", SeekingImmortalsMod.MODID + ":secret_realm_fallen_demon");
        map.put("fallen_demon_depths", SeekingImmortalsMod.MODID + ":secret_realm_fallen_demon");
        map.put("asura_realm", SeekingImmortalsMod.MODID + ":asura_realm");
        map.put("immortal_realm", SeekingImmortalsMod.MODID + ":immortal_realm");
        return Map.copyOf(map);
    }

    private SecretRealmDimensionService() {}

    public static Optional<String> dimensionIdFor(String realmId) {
        if (realmId == null || realmId.isBlank()) {
            return Optional.empty();
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (REALM_DIMENSIONS.containsKey(id)) {
            return Optional.of(REALM_DIMENSIONS.get(id));
        }
        if (id.contains("asura") || id.contains("xiuluo")) {
            return Optional.of(REALM_DIMENSIONS.get("asura_realm"));
        }
        if (id.contains("immortal") || id.contains("xianjie") || id.contains("true_immortal")) {
            return Optional.of(REALM_DIMENSIONS.get("immortal_realm"));
        }
        if (id.contains("blood")) {
            return Optional.of(REALM_DIMENSIONS.get("blood_forbidden"));
        }
        if (id.contains("void") || id.contains("palace")) {
            return Optional.of(REALM_DIMENSIONS.get("void_palace"));
        }
        if (id.contains("mist") || id.contains("cave")) {
            return Optional.of(REALM_DIMENSIONS.get("mist_cave_trial"));
        }
        if (id.contains("demon") || id.contains("fallen")) {
            return Optional.of(REALM_DIMENSIONS.get("fallen_demon_valley"));
        }
        return Optional.empty();
    }

    public static boolean hasDedicatedDimension(ServerPlayer player, String realmId) {
        Optional<String> dimId = dimensionIdFor(realmId);
        if (dimId.isEmpty() || player == null) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(dimId.get());
        if (location == null) {
            return false;
        }
        return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location)) != null;
    }

    public static boolean teleportInto(ServerPlayer player, String realmId) {
        Optional<String> dimId = dimensionIdFor(realmId);
        if (dimId.isEmpty()) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(dimId.get());
        if (location == null) {
            return false;
        }
        ServerLevel target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        if (target == null) {
            return false;
        }
        int x = 0;
        int z = 0;
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        y = Math.max(target.getMinBuildHeight() + 2, Math.min(target.getMaxBuildHeight() - 2, y));
        BlockPos base = new BlockPos(x, y - 1, z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                target.setBlock(base.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                target.setBlock(base.offset(dx, 1, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                target.setBlock(base.offset(dx, 2, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        player.teleportTo(target, x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    public static int dedicatedDimensionCount() {
        return REALM_DIMENSIONS.size();
    }
}
