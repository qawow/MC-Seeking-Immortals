package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.block.entity.FormationCoreBlockEntity;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.structure.SpiritGatheringFormationStructure;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Spirit gathering formation core with persistent BlockEntity (Wave54).
 */
public class SpiritGatheringFormationCoreBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public SpiritGatheringFormationCoreBlock(Properties properties) {
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FormationCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FORMATION_CORE.get(), FormationCoreBlockEntity::serverTick);
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
                    "message.seeking_immortals.spirit_gathering_formation_core.info"), false);
            return InteractionResult.CONSUME;
        }

        SpiritGatheringFormationStructure.CheckResult check = SpiritGatheringFormationStructure.validate(
                level,
                pos,
                ModBlocks.SPIRIT_GATHERING_ARRAY.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spirit_gathering_formation_core.incomplete",
                    check.missingRing()), false);
            return InteractionResult.CONSUME;
        }

        serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        FormationFieldService.activate(serverPlayer.serverLevel(), pos, FormationFieldService.FieldKind.SPIRIT_GATHER, serverPlayer);
        if (level.getBlockEntity(pos) instanceof FormationCoreBlockEntity core) {
            core.activate(FormationFieldService.FieldKind.SPIRIT_GATHER, 20 * 90, false);
        }
        playActivationEffects(serverPlayer.serverLevel(), pos);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.spirit_gathering_formation_core.activated"), true);
        return InteractionResult.CONSUME;
    }

    private void playActivationEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.4D, z, 32, 0.85D, 0.45D, 0.85D, 0.02D);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5D, z, 40, 0.75D, 0.55D, 0.75D, 0.05D);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.05F);
    }
}
