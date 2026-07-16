package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.craft.RefinementForgeCraftHelper;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.RefinementForgeG3Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/**
 * Text-material refinement_forge_g3 (forge grade 3).
 */
public class RefinementForgeG3Block extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public RefinementForgeG3Block(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge_g3.info"), false);
            return InteractionResult.CONSUME;
        }
        RefinementForgeG3Structure.CheckResult check = RefinementForgeG3Structure.validate(
                level, pos, ModBlocks.REFINEMENT_FORGE_G3.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.refinement_forge_g3.incomplete",
                    check.missingRing(),
                    check.missingFrame(),
                    check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        RefinementForgeCraftHelper.tryCraft(serverPlayer, pos, 3,
                "message.seeking_immortals.refinement_forge_g3.activated",
                "message.seeking_immortals.refinement_forge_g3.no_recipe");
        return InteractionResult.CONSUME;
    }
}
