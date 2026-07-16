package com.xunxian.seekingimmortals.craft;

import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.recipe.RefinementCraftingRecipe;
import com.xunxian.seekingimmortals.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

/**
 * Shared G1–G3 forge craft path: prefer datapack {@link RefinementCraftingRecipe}
 * filtered by forge grade, then fall back to {@link ArtifactRefinementService}.
 */
public final class RefinementForgeCraftHelper {
    private RefinementForgeCraftHelper() {}

    public static boolean tryCraft(ServerPlayer player, BlockPos pos, int forgeGrade, String activatedKey, String noRecipeKey) {
        ServerLevel serverLevel = player.serverLevel();
        SimpleContainer inv = new SimpleContainer(36);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, player.getInventory().items.get(i).copy());
        }
        Optional<RefinementCraftingRecipe> optional = serverLevel.getRecipeManager()
                .getAllRecipesFor(ModRecipes.REFINEMENT_TYPE.get()).stream()
                .filter(recipe -> recipe.forgeGrade() <= forgeGrade)
                .filter(recipe -> recipe.matches(inv, serverLevel))
                .findFirst();
        if (optional.isPresent()) {
            RefinementCraftingRecipe recipe = optional.get();
            boolean success = player.getAbilities().instabuild
                    || serverLevel.random.nextFloat() < recipe.successRate();
            if (!player.getAbilities().instabuild) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    boolean consumed = false;
                    for (int i = 0; i < player.getInventory().items.size(); i++) {
                        ItemStack stack = player.getInventory().items.get(i);
                        if (!stack.isEmpty() && ingredient.test(stack)) {
                            stack.shrink(1);
                            consumed = true;
                            break;
                        }
                    }
                    if (!consumed) {
                        player.displayClientMessage(Component.translatable(noRecipeKey), false);
                        return false;
                    }
                }
            }
            if (success) {
                ItemStack out = recipe.assemble(inv, serverLevel.registryAccess());
                if (!player.getInventory().add(out.copy())) {
                    player.drop(out.copy(), false);
                }
                serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        12 + forgeGrade * 2, 0.35D, 0.25D, 0.35D, 0.01D);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F - forgeGrade * 0.05F);
                player.displayClientMessage(Component.translatable(activatedKey), true);
            } else {
                var salvage = ArtifactRefinementService.grantDefaultFailureLoot(player, "low");
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.refinement_forge.failed",
                        recipe.getId().toString(),
                        String.format(java.util.Locale.ROOT, "%.0f%%", recipe.successRate() * 100.0F),
                        salvage.isEmpty() ? "-" : salvage.get(0).getHoverName().getString()), false);
            }
            player.containerMenu.broadcastChanges();
            return true;
        }

        String recipeId = ArtifactRefinementService.selectRecipeId(player, forgeGrade);
        if (recipeId == null || recipeId.isBlank()) {
            player.displayClientMessage(Component.translatable(noRecipeKey), false);
            return false;
        }
        boolean ok = ArtifactRefinementService.refine(player, recipeId, forgeGrade);
        if (ok) {
            serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    12 + forgeGrade * 2, 0.35D, 0.25D, 0.35D, 0.01D);
            serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F - forgeGrade * 0.05F);
            player.displayClientMessage(Component.translatable(activatedKey), true);
        }
        return ok;
    }
}
