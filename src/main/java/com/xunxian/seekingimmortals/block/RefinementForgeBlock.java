package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.craft.RefinementForgeCraftHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.RefinementForgeStructure;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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
 * Text-material refinement forge workstation (G1).
 * Sneak-use validates multiblock then crafts via datapack serializer / catalog fallback.
 */
public class RefinementForgeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public RefinementForgeBlock(Properties properties) {
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
            var snapshot = com.xunxian.seekingimmortals.artifact.ArtifactDataService.builtin();
            java.util.List<String> lines = new java.util.ArrayList<>();
            int n = 0;
            for (var recipe : snapshot.refinementRecipes().values()) {
                if (recipe == null) {
                    continue;
                }
                Realm requiredRealm = Realm.fromDesignId(recipe.realmMin());
                String recipeName = PlayerDisplayText.isSafe(recipe.display())
                        ? recipe.display().trim() : "未知炼器配方";
                String realmName = requiredRealm == null ? "未知境界" : requiredRealm.getDisplayName();
                lines.add(recipeName + " | " + realmName + " | "
                        + String.format(java.util.Locale.ROOT, "%.0f%%",
                        Math.max(0.0D, Math.min(1.0D, recipe.baseSuccessRate())) * 100.0D));
                if (++n >= 16) {
                    break;
                }
            }
            if (lines.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.info"), false);
            } else {
                com.xunxian.seekingimmortals.network.OpenRefinePlanPacket.send(serverPlayer, lines);
            }
            return InteractionResult.CONSUME;
        }
        RefinementForgeStructure.CheckResult check =
                RefinementForgeStructure.validate(level, pos, ModBlocks.REFINEMENT_FORGE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.refinement_forge.incomplete",
                    check.missingBaseBlocks(),
                    check.missingFrameBlocks()), false);
            return InteractionResult.CONSUME;
        }
        if (!com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .ensureCommissioned(serverPlayer, "refinement_forge_g1", pos)) {
            return InteractionResult.CONSUME;
        }

        RefinementForgeCraftHelper.tryCraft(
                serverPlayer, pos, 1,
                "message.seeking_immortals.refinement_forge.activated",
                "message.seeking_immortals.refinement_forge.no_recipe");
        return InteractionResult.CONSUME;
    }
}
