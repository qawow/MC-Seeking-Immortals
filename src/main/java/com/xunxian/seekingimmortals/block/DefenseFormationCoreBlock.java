package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.DefenseFormationStructure;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
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

/**
 * Defense formation core. Normal use shows info; sneak-use validates a radius-2
 * outer ring of SPIRIT_ORE blocks and grants short defensive buffs when complete.
 */
public class DefenseFormationCoreBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public DefenseFormationCoreBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.defense_formation_core.info"), false);
            return InteractionResult.CONSUME;
        }

        DefenseFormationStructure.CheckResult check = DefenseFormationStructure.validate(
                level,
                pos,
                ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.defense_formation_core.incomplete",
                    check.missingRing()), false);
            return InteractionResult.CONSUME;
        }

        serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        FormationFieldService.activate(serverPlayer.serverLevel(), pos, FormationFieldService.FieldKind.DEFENSE, serverPlayer);
        playActivationEffects(serverPlayer.serverLevel(), pos);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.defense_formation_core.activated"), true);
        return InteractionResult.CONSUME;
    }

    private void playActivationEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.CRIT, x, y + 0.4D, z, 40, 0.75D, 0.45D, 0.75D, 0.15D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.85F, 0.85F);
    }
}
