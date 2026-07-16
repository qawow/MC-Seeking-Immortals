package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * M07-style formed check for immortal_teleport_grand_array.
 * Reuses long-range 9x9 + pillars semantics via {@link LongRangeTeleportArrayStructure}.
 */
public final class ImmortalTeleportGrandArrayStructure {
    private ImmortalTeleportGrandArrayStructure() {}

    public static LongRangeTeleportArrayStructure.CheckResult validate(Level level, BlockPos center,
                                                                       Block ringBlock, Block frameBlock) {
        return LongRangeTeleportArrayStructure.validate(level, center, ringBlock, frameBlock);
    }

    public static boolean isFormed(Level level, BlockPos center, Block ringBlock, Block frameBlock) {
        return validate(level, center, ringBlock, frameBlock).complete();
    }
}
