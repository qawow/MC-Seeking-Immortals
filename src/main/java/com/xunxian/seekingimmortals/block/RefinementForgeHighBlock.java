package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.craft.RefinementForgeCraftHelper;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.RefinementForgeHighStructure;
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
 * High-tier refinement forge controller (G4–G6).
 */
public class RefinementForgeHighBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    private final int grade;
    private final String stationId;

    public RefinementForgeHighBlock(Properties properties, int grade) {
        super(properties);
        this.grade = Math.max(4, Math.min(6, grade));
        this.stationId = "refinement_forge_g" + this.grade;
    }

    public int grade() {
        return grade;
    }

    public String stationId() {
        return stationId;
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
        String key = "message.seeking_immortals." + stationId;
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable(key + ".info"), false);
            return InteractionResult.CONSUME;
        }
        Block self = level.getBlockState(pos).getBlock();
        RefinementForgeHighStructure.CheckResult check = RefinementForgeHighStructure.validate(
                level, pos, self, ModBlocks.SPIRIT_ORE.get(), grade);
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    key + ".incomplete",
                    check.missingRing(),
                    check.missingFrame(),
                    check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }
        if (!com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .ensureCommissioned(serverPlayer, stationId, pos)
                && !com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .ensureCommissioned(serverPlayer, "refinement_forge", pos)) {
            return InteractionResult.CONSUME;
        }
        RefinementForgeCraftHelper.tryCraft(serverPlayer, pos, grade,
                key + ".activated",
                key + ".no_recipe");
        return InteractionResult.CONSUME;
    }
}
