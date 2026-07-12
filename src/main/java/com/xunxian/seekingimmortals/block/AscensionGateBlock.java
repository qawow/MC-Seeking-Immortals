package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.AscensionGateStructure;
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
 * Text-material ascension_gate. Sneak-use validates the tall gate multiblock then routes
 * through WorldpackGameplayService.usePortalArray (ticket/realm gates deferred).
 */
public class AscensionGateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);

    public AscensionGateBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension_gate.info"), false);
            return InteractionResult.CONSUME;
        }
        AscensionGateStructure.CheckResult check = AscensionGateStructure.validate(
                level, pos, ModBlocks.ASCENSION_GATE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ascension_gate.incomplete",
                    check.missingRingBlocks(),
                    check.missingFrameBlocks(),
                    check.blockedApertureBlocks()), false);
            return InteractionResult.CONSUME;
        }
        ServerLevel origin = serverPlayer.serverLevel();
        BlockPos originPos = pos.immutable();
        if (WorldpackGameplayService.usePortalArray(serverPlayer)) {
            origin.sendParticles(ParticleTypes.END_ROD, originPos.getX() + 0.5D, originPos.getY() + 1.2D, originPos.getZ() + 0.5D,
                    60, 1.1D, 1.2D, 1.1D, 0.03D);
            origin.playSound(null, originPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.55F, 1.35F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ascension_gate.activated"), true);
        }
        return InteractionResult.CONSUME;
    }
}
