package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 昆吾寒罡锻台结构验证（3×3×2 特殊炼器台）。
 * 结构：冰晶基座 + 锻台中心 + 四周寒气柱。
 */
public final class KunwuFrostForgeStructure {
    public static final int BASE_RADIUS = 1;
    public static final int FORGE_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> FROST_PILLAR_OFFSETS = buildFrostPillarOffsets();
    private static final BlockPos FORGE_CENTER = new BlockPos(0, 1, 0);

    private KunwuFrostForgeStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block frostBlock, Block forgeBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingFrost = 0;
        for (BlockPos offset : FROST_PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(frostBlock)) {
                missingFrost++;
            }
        }

        boolean forgePresent = level.getBlockState(center.offset(FORGE_CENTER)).is(forgeBlock);

        return new CheckResult(missingBase, missingFrost, forgePresent ? 0 : 1);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 3×3 基座
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFrostPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四个方向的寒气柱
        offsets.add(new BlockPos(-BASE_RADIUS, 1, 0));
        offsets.add(new BlockPos(BASE_RADIUS, 1, 0));
        offsets.add(new BlockPos(0, 1, -BASE_RADIUS));
        offsets.add(new BlockPos(0, 1, BASE_RADIUS));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingFrost, int missingForge) {
        public boolean complete() {
            return missingBase <= 0 && missingFrost <= 0 && missingForge <= 0;
        }
    }
}
