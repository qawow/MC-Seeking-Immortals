package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 传送门结构验证（适用于星宫巡海传送门、逆星暗港趸船阵等传送类结构）。
 * 结构：5×5 基座 + 四角传送柱。
 */
public final class TeleportGateStructure {
    public static final int BASE_RADIUS = 2;
    public static final int PILLAR_HEIGHT = 2;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> PILLAR_OFFSETS = buildPillarOffsets();

    private TeleportGateStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block pillarBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingPillar = 0;
        for (BlockPos offset : PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillar++;
            }
        }
        return new CheckResult(missingBase, missingPillar);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= PILLAR_HEIGHT; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillar) {
        public boolean complete() {
            return missingBase <= 0 && missingPillar <= 0;
        }
    }
}
