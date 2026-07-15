package com.xunxian.seekingimmortals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wave500: alchemy lids are placeable multiblock parts (sit on top of furnace controller).
 */
public class AlchemyLidBlock extends Block {
    private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D);
    private final int tier;

    public AlchemyLidBlock(Properties properties, int tier) {
        super(properties);
        this.tier = Math.max(1, tier);
    }

    public int tier() {
        return tier;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
