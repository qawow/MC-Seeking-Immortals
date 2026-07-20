package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 封印柱结构验证（适用于昆吾封镇柱、据点方尖碑等柱状封印类结构）。
 * 结构：1×1 基座 + 垂直柱身 + 顶部封印核心。
 */
public final class SealPillarStructure {
    public static final int PILLAR_HEIGHT = 4;

    private static final BlockPos BASE_OFFSET = BlockPos.ZERO;
    private static final List<BlockPos> PILLAR_BODY_OFFSETS = buildPillarBodyOffsets();
    private static final BlockPos SEAL_CORE_OFFSET = new BlockPos(0, PILLAR_HEIGHT - 1, 0);

    private SealPillarStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block pillarBlock, Block sealCoreBlock) {
        boolean missingBase = !level.getBlockState(center.offset(BASE_OFFSET)).is(pillarBlock);

        int missingPillarBody = 0;
        for (BlockPos offset : PILLAR_BODY_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillarBody++;
            }
        }

        boolean missingSealCore = !level.getBlockState(center.offset(SEAL_CORE_OFFSET)).is(sealCoreBlock);

        return new CheckResult(missingBase, missingPillarBody, missingSealCore);
    }

    private static List<BlockPos> buildPillarBodyOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y < PILLAR_HEIGHT - 1; y++) {
            offsets.add(new BlockPos(0, y, 0));
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(boolean missingBase, int missingPillarBody, boolean missingSealCore) {
        public boolean complete() {
            return !missingBase && missingPillarBody <= 0 && !missingSealCore;
        }
    }
}
