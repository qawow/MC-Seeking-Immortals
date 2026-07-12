package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class SpiritGatheringArrayBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);

    public SpiritGatheringArrayBlock(Properties properties) {
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
        if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            PortalArrayStructure.CheckResult check = PortalArrayStructure.validate(level, pos, this, ModBlocks.SPIRIT_ORE.get());
            if (!check.complete()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.spirit_gathering_array.portal_incomplete",
                        check.missingBaseBlocks(),
                        check.missingFrameBlocks(),
                        check.blockedApertureBlocks()), false);
                return InteractionResult.CONSUME;
            }
            ServerLevel originLevel = serverPlayer.serverLevel();
            BlockPos originPos = pos.immutable();
            if (WorldpackGameplayService.usePortalArray(serverPlayer)) {
                playPortalEffects(originLevel, originPos, true);
                playPortalEffects(serverPlayer.serverLevel(), serverPlayer.blockPosition(), false);
            }
            return InteractionResult.CONSUME;
        }
        if (level instanceof ServerLevel serverLevel) {
            SpiritualAuraManager.AuraInfo aura = SpiritualAuraManager.getAuraInfo(serverLevel, pos);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_gathering_array.use", aura.formationBonus(), aura.concentration()), false);
        }
        return InteractionResult.CONSUME;
    }

    private void playPortalEffects(ServerLevel level, BlockPos pos, boolean departure) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.PORTAL, x, y + 0.6D, z, 96, 1.55D, 1.35D, 1.55D, 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.8D, z, 32, 0.95D, 1.05D, 0.95D, 0.02D);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.75F, departure ? 0.85F : 1.25F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, departure ? 1.0F : 1.55F);
    }
}
