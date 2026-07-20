package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 秘境门户/入口结构验证（适用于虚天殿门残件、阴阳窟窟门等 3×1×4 门类结构）。
 * 结构：双柱门框 + 顶部横梁 + 中心传送核心。
 */
public final class SecretRealmGateStructure {
    public static final int GATE_WIDTH = 3;
    public static final int GATE_HEIGHT = 4;

    private static final List<BlockPos> LEFT_PILLAR_OFFSETS = buildLeftPillarOffsets();
    private static final List<BlockPos> RIGHT_PILLAR_OFFSETS = buildRightPillarOffsets();
    private static final List<BlockPos> LINTEL_OFFSETS = buildLintelOffsets();
    private static final BlockPos CORE = new BlockPos(0, 1, 0);

    private SecretRealmGateStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block pillarBlock, Block lintelBlock, Block coreBlock) {
        int missingLeft = 0;
        for (BlockPos offset : LEFT_PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingLeft++;
            }
        }

        int missingRight = 0;
        for (BlockPos offset : RIGHT_PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingRight++;
            }
        }

        int missingLintel = 0;
        for (BlockPos offset : LINTEL_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(lintelBlock)) {
                missingLintel++;
            }
        }

        boolean corePresent = level.getBlockState(center.offset(CORE)).is(coreBlock);

        return new CheckResult(missingLeft, missingRight, missingLintel, corePresent ? 0 : 1);
    }

    private static List<BlockPos> buildLeftPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 左侧门柱
        for (int y = 0; y < GATE_HEIGHT - 1; y++) {
            offsets.add(new BlockPos(-1, y, 0));
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildRightPillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 右侧门柱
        for (int y = 0; y < GATE_HEIGHT - 1; y++) {
            offsets.add(new BlockPos(1, y, 0));
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildLintelOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 顶部横梁（3格宽）
        for (int x = -1; x <= 1; x++) {
            offsets.add(new BlockPos(x, GATE_HEIGHT - 1, 0));
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingLeft, int missingRight, int missingLintel, int missingCore) {
        public boolean complete() {
            return missingLeft <= 0 && missingRight <= 0 && missingLintel <= 0 && missingCore <= 0;
        }
    }
}
