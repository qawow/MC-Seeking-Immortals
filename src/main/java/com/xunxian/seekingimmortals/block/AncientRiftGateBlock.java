package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.AncientRiftGateStructure;
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
 * Text-material ancient_rift gate. Sneak-use validates multiblock then routes to
 * fallen_demon_valley via WorldpackGameplayService (portal fee path).
 */
public class AncientRiftGateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public AncientRiftGateBlock(Properties properties) {
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.ancient_rift_gate.info"), false);
            return InteractionResult.CONSUME;
        }
        AncientRiftGateStructure.CheckResult check = AncientRiftGateStructure.validate(
                level, pos, ModBlocks.ANCIENT_RIFT_GATE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ancient_rift_gate.incomplete",
                    check.missingRing(), check.missingFrame(), check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        ServerLevel originLevel = serverPlayer.serverLevel();
        BlockPos originPos = pos.immutable();
        if (WorldpackGameplayService.useAncientRiftGate(serverPlayer)) {
            playPortalEffects(originLevel, originPos, true);
            playPortalEffects(serverPlayer.serverLevel(), serverPlayer.blockPosition(), false);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ancient_rift_gate.activated"), true);
        }
        return InteractionResult.CONSUME;
    }

    private void playPortalEffects(ServerLevel level, BlockPos pos, boolean departure) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.FLAME, x, y + 0.4D, z, 24, 0.7D, 0.4D, 0.7D, 0.02D);
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.55D, z, 18, 0.55D, 0.4D, 0.55D, 0.01D);
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, departure ? 0.7F : 1.2F);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.55F, departure ? 0.65F : 1.35F);
    }
}
