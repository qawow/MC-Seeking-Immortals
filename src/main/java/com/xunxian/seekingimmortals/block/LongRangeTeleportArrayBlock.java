package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.LongRangeTeleportArrayStructure;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Text-material teleport_array_long_range. Larger multiblock than fixed teleport;
 * sneak-use validates then routes through WorldpackGameplayService.usePortalArray.
 */
public class LongRangeTeleportArrayBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);

    public LongRangeTeleportArrayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.long_range_teleport_array.info"), false);
            return InteractionResult.CONSUME;
        }
        LongRangeTeleportArrayStructure.CheckResult check = LongRangeTeleportArrayStructure.validate(
                level, pos, ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.long_range_teleport_array.incomplete",
                    check.missingRing(),
                    check.missingFrame(),
                    check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        ServerLevel origin = serverPlayer.serverLevel();
        BlockPos originPos = pos.immutable();
        if (WorldpackGameplayService.usePortalArray(serverPlayer)) {
            origin.sendParticles(ParticleTypes.ENCHANT, originPos.getX() + 0.5D, originPos.getY() + 1.3D, originPos.getZ() + 0.5D,
                    72, 1.3D, 1.0D, 1.3D, 0.04D);
            origin.sendParticles(ParticleTypes.END_ROD, originPos.getX() + 0.5D, originPos.getY() + 1.5D, originPos.getZ() + 0.5D,
                    28, 0.8D, 0.8D, 0.8D, 0.02D);
            origin.playSound(null, originPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7F, 0.85F);
            origin.playSound(null, originPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, 0.75F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.long_range_teleport_array.activated"), true);
        }
        return InteractionResult.CONSUME;
    }
}
