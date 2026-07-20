package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼器火炉结构验证（适用于灵火铜鼎、昆吾寒罡锻台等供火类结构）。
 * 结构：1×1 基座 + 垂直炉身 + 顶部火口。
 */
public final class BrazierStructure {
    public static final int BRAZIER_HEIGHT = 2;

    private static final BlockPos BASE_OFFSET = BlockPos.ZERO;
    private static final BlockPos BODY_OFFSET = new BlockPos(0, 1, 0);
    private static final BlockPos FIRE_MOUTH_OFFSET = new BlockPos(0, BRAZIER_HEIGHT, 0);

    private BrazierStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block brazierBlock, Block fireBlock) {
        boolean missingBase = !level.getBlockState(center.offset(BASE_OFFSET)).is(brazierBlock);
        boolean missingBody = !level.getBlockState(center.offset(BODY_OFFSET)).is(brazierBlock);
        boolean missingFire = !level.getBlockState(center.offset(FIRE_MOUTH_OFFSET)).is(fireBlock);

        return new CheckResult(missingBase, missingBody, missingFire);
    }

    public record CheckResult(boolean missingBase, boolean missingBody, boolean missingFire) {
        public boolean complete() {
            return !missingBase && !missingBody && !missingFire;
        }
    }
}
