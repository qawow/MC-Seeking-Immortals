package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.recipe.RefinementCraftingRecipe;
import com.xunxian.seekingimmortals.registry.ModRecipes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.RefinementForgeStructure;
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
 * Text-material refinement forge workstation.
 * Sneak-use validates multiblock then attempts ArtifactRefinementService.refine with first catalog recipe.
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
                lines.add(recipe.id() + " | " + recipe.display() + " | "
                        + (recipe.realmMin() == null ? "?" : recipe.realmMin()) + " | "
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

        // Wave54: prefer custom RecipeSerializer refinement recipes from RecipeManager.
        ServerLevel serverLevel = serverPlayer.serverLevel();
        SimpleContainer inv = new SimpleContainer(36);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, serverPlayer.getInventory().items.get(i).copy());
        }
        var optional = serverLevel.getRecipeManager()
                .getRecipeFor(ModRecipes.REFINEMENT_TYPE.get(), inv, serverLevel);
        if (optional.isPresent()) {
            RefinementCraftingRecipe recipe = optional.get();
            boolean success = serverPlayer.getAbilities().instabuild
                    || serverLevel.random.nextFloat() < recipe.successRate();
            // consume one of each ingredient from player inventory
            if (!serverPlayer.getAbilities().instabuild) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    boolean consumed = false;
                    for (int i = 0; i < serverPlayer.getInventory().items.size(); i++) {
                        ItemStack stack = serverPlayer.getInventory().items.get(i);
                        if (!stack.isEmpty() && ingredient.test(stack)) {
                            stack.shrink(1);
                            consumed = true;
                            break;
                        }
                    }
                    if (!consumed) {
                        player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.no_recipe"), false);
                        return InteractionResult.CONSUME;
                    }
                }
            }
            if (success) {
                ItemStack out = recipe.assemble(inv, serverLevel.registryAccess());
                if (!serverPlayer.getInventory().add(out.copy())) {
                    serverPlayer.drop(out.copy(), false);
                }
                serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        12, 0.35D, 0.25D, 0.35D, 0.01D);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.activated"), true);
            } else {
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.refine.failure",
                        recipe.getId().toString(), String.format(java.util.Locale.ROOT, "%.0f%%", recipe.successRate() * 100.0F), "-"), false);
            }
            serverPlayer.containerMenu.broadcastChanges();
            return InteractionResult.CONSUME;
        }

        // Fallback to catalog service path.
        String recipeId = ArtifactRefinementService.selectRecipeId(serverPlayer);
        if (recipeId == null || recipeId.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.no_recipe"), false);
            return InteractionResult.CONSUME;
        }

        boolean ok = ArtifactRefinementService.refine(serverPlayer, recipeId);
        if (ok) {
            serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    12, 0.35D, 0.25D, 0.35D, 0.01D);
            serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.activated"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.no_recipe"), false);
        }
        return InteractionResult.CONSUME;
    }
}
