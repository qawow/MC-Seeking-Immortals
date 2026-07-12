package com.xunxian.seekingimmortals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 御剑/土系法术召唤的临时土墙。放置时由施法方安排 {@link #scheduleRemoval}，到期后 {@link #tick}
 * 自行设回 air，杜绝永久改地形与刷石漏洞（EarthWall 修复项）。
 */
public class EarthWallBlock extends Block {
    public static final int REMOVAL_TICKS = 200;

    public EarthWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        level.removeBlock(pos, false);
    }
}
