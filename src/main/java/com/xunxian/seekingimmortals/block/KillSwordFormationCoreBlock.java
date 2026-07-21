package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.structure.ArrayHubStructure;
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

/** formation_catalog kill_sword placeable core. */
public class KillSwordFormationCoreBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public KillSwordFormationCoreBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.kill_sword_formation_core.info"), false);
            return InteractionResult.CONSUME;
        }
        ArrayHubStructure.CheckResult check = ArrayHubStructure.validateKillHub(level, pos);
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.kill_sword_formation_core.incomplete", check.missingTotal()), false);
            return InteractionResult.CONSUME;
        }
        if (!FormationFieldService.activate(
                serverPlayer.serverLevel(),
                pos,
                FormationFieldService.FieldKind.KILL_SWORD,
                serverPlayer,
                "kill_sword")) {
            ArrayHubStructure.CheckResult retry = ArrayHubStructure.validateKillHub(level, pos);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.kill_sword_formation_core.incomplete", retry.missingTotal()), false);
            return InteractionResult.CONSUME;
        }
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        ServerLevel serverLevel = serverPlayer.serverLevel();
        serverLevel.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                40, 0.7D, 0.5D, 0.7D, 0.08D);
        serverLevel.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.8F, 0.9F);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.kill_sword_formation_core.activated"), true);
        return InteractionResult.CONSUME;
    }
}
