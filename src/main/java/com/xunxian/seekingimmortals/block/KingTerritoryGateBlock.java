package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.KingTerritoryGateStructure;
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
 * Shared placeable for 7 king_territory spatial nodes (all barbarian wasteland).
 */
public class KingTerritoryGateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public KingTerritoryGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.CONSUME;
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.king_territory_gate.info"), false);
            return InteractionResult.CONSUME;
        }
        KingTerritoryGateStructure.CheckResult check = KingTerritoryGateStructure.validate(
                level, pos, ModBlocks.KING_TERRITORY_GATE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.king_territory_gate.incomplete",
                    check.missingRing(), check.missingFrame(), check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        ServerLevel originLevel = serverPlayer.serverLevel();
        if (WorldpackGameplayService.useKingTerritoryGate(serverPlayer)) {
            originLevel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    24, 0.7D, 0.4D, 0.7D, 0.02D);
            originLevel.playSound(null, pos, SoundEvents.RAVAGER_ROAR, SoundSource.BLOCKS, 0.35F, 1.3F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.king_territory_gate.activated"), true);
        }
        return InteractionResult.CONSUME;
    }
}
