package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.structure.RingFormationStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** formation_catalog seal_demon_array placeable core. */
public class SealDemonFormationCoreBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public SealDemonFormationCoreBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.seal_demon_formation_core.info"), false);
            return InteractionResult.CONSUME;
        }
        RingFormationStructure.CheckResult check = RingFormationStructure.validate(level, pos, ModBlocks.SPIRIT_ORE.get(), 2);
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.seal_demon_formation_core.incomplete", check.missingRing()), false);
            return InteractionResult.CONSUME;
        }
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 1));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
        FormationFieldService.activate(serverPlayer.serverLevel(), pos, FormationFieldService.FieldKind.SEAL_DEMON, serverPlayer);
        ServerLevel serverLevel = serverPlayer.serverLevel();
        serverLevel.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                28, 0.7D, 0.4D, 0.7D, 0.02D);
        serverLevel.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.45F, 1.4F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.seal_demon_formation_core.activated"), true);
        return InteractionResult.CONSUME;
    }
}
