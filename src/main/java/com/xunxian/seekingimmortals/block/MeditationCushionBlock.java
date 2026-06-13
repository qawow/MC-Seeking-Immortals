package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MeditationCushionBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 6.0D, 15.0D);

    public MeditationCushionBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (player.isPassenger()) {
            return InteractionResult.CONSUME;
        }
        AABB seatArea = new AABB(pos).inflate(0.25D, 0.75D, 0.25D);
        boolean occupied = serverLevel.getEntitiesOfClass(CushionSeatEntity.class, seatArea, seat -> seat.blockPosition().equals(pos) && !seat.getPassengers().isEmpty()).size() > 0;
        if (occupied) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.cushion.occupied"), true);
            return InteractionResult.CONSUME;
        }
        CushionSeatEntity seat = new CushionSeatEntity(serverLevel, pos);
        serverLevel.addFreshEntity(seat);
        player.startRiding(seat, true);
        CultivationHelper.get(player).ifPresent(cultivation -> cultivation.setMeditating(true));
        return InteractionResult.CONSUME;
    }
}
