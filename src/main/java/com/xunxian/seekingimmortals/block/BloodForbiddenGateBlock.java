package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.BloodForbiddenGateStructure;
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
 * Text-material blood_forbidden_gate cycle portal. Multiblock gate routes through
 * WorldpackGameplayService.usePortalArray; dedicated blood_forbidden_land target deferred.
 */
public class BloodForbiddenGateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);

    public BloodForbiddenGateBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.blood_forbidden_gate.info"), false);
            return InteractionResult.CONSUME;
        }
        BloodForbiddenGateStructure.CheckResult check = BloodForbiddenGateStructure.validate(
                level, pos, ModBlocks.BLOOD_FORBIDDEN_GATE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.blood_forbidden_gate.incomplete",
                    check.missingRing(),
                    check.missingFrame(),
                    check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        ServerLevel origin = serverPlayer.serverLevel();
        BlockPos originPos = pos.immutable();
        if (WorldpackGameplayService.usePortalArray(serverPlayer)) {
            origin.sendParticles(ParticleTypes.CRIMSON_SPORE, originPos.getX() + 0.5D, originPos.getY() + 1.2D, originPos.getZ() + 0.5D,
                    48, 1.0D, 0.9D, 1.0D, 0.02D);
            origin.sendParticles(ParticleTypes.SOUL, originPos.getX() + 0.5D, originPos.getY() + 1.0D, originPos.getZ() + 0.5D,
                    24, 0.7D, 0.6D, 0.7D, 0.01D);
            origin.playSound(null, originPos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.45F, 0.8F);
            origin.playSound(null, originPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.55F, 0.7F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.blood_forbidden_gate.activated"), true);
        }
        return InteractionResult.CONSUME;
    }
}
