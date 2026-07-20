package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 小型控制/交互结构通用验证器。
 * 适用于：虚天钥孔台、千竹总控台、结构修复台、轮回殿交易案、掌天瓶承架等。
 * 尺寸：1×1×2 或 2×2×2。
 */
public final class SmallControlStructure {
    private SmallControlStructure() {}

    /**
     * 验证 1×1×2 单柱控制台（钥孔台、瓶承架）
     */
    public static CheckResult validateSinglePost(Level level, BlockPos center, Block baseBlock, Block controlBlock) {
        boolean basePresent = level.getBlockState(center).is(baseBlock);
        boolean controlPresent = level.getBlockState(center.above()).is(controlBlock);

        return new CheckResult(basePresent ? 0 : 1, controlPresent ? 0 : 1);
    }

    /**
     * 验证 2×2×2 多方块控制台（千竹总控台、结构修复台）
     */
    public static CheckResult validate2x2x2(Level level, BlockPos center, Block baseBlock, Block coreBlock, Block panelBlock) {
        List<BlockPos> baseOffsets = build2x2Base();
        List<BlockPos> panelOffsets = build2x2Panel();

        int missingBase = 0;
        for (BlockPos offset : baseOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingPanels = 0;
        for (BlockPos offset : panelOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(panelBlock)) {
                missingPanels++;
            }
        }

        // 中心控制核心
        boolean corePresent = level.getBlockState(center.offset(0, 1, 0)).is(coreBlock);

        return new CheckResult(missingBase, missingPanels + (corePresent ? 0 : 1));
    }

    /**
     * 验证 2×1×2 交易案结构
     */
    public static CheckResult validateTradingDesk(Level level, BlockPos center, Block deskBlock, Block displayBlock) {
        // 2×2 底座
        List<BlockPos> baseOffsets = build2x2Base();
        int missingDesk = 0;
        for (BlockPos offset : baseOffsets) {
            if (!level.getBlockState(center.offset(offset)).is(deskBlock)) {
                missingDesk++;
            }
        }

        // 中心展示台
        boolean displayPresent = level.getBlockState(center.offset(0, 1, 0)).is(displayBlock);

        return new CheckResult(missingDesk, displayPresent ? 0 : 1);
    }

    private static List<BlockPos> build2x2Base() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> build2x2Panel() {
        List<BlockPos> offsets = new ArrayList<>();
        // 外围三个面板位置（不含中心）
        offsets.add(new BlockPos(0, 1, 1));
        offsets.add(new BlockPos(1, 1, 0));
        offsets.add(new BlockPos(1, 1, 1));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingPrimary, int missingSecondary) {
        public boolean complete() {
            return missingPrimary <= 0 && missingSecondary <= 0;
        }
    }
}
