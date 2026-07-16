package com.xunxian.seekingimmortals.craft;

import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.recipe.RefinementCraftingRecipe;
import com.xunxian.seekingimmortals.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;

import java.util.Optional;

/**
 * Shared G1–G3 forge craft path. Datapack recipes select a candidate, while
 * {@link ArtifactRefinementService} remains the single authority for realm,
 * manual, material, success-rate, ownership, spirit and progression rules.
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
            String recipeId = catalogRecipeId(optional.get());
            if (ArtifactDataService.builtin().findRecipe(recipeId).isEmpty()) {
                player.displayClientMessage(Component.translatable(noRecipeKey), false);
                return false;
            }
            return refineAuthoritatively(player, pos, forgeGrade, activatedKey, recipeId);
        }

        String recipeId = ArtifactRefinementService.selectRecipeId(player, forgeGrade);
        if (recipeId == null || recipeId.isBlank()) {
            player.displayClientMessage(Component.translatable(noRecipeKey), false);
            return false;
        }
        return refineAuthoritatively(player, pos, forgeGrade, activatedKey, recipeId);
    }

    static String catalogRecipeId(RefinementCraftingRecipe recipe) {
        String path = recipe.getId().getPath();
        path = path.endsWith("_serializer")
                ? path.substring(0, path.length() - "_serializer".length())
                : path;
        return path.startsWith("refinement_")
                ? "refine_" + path.substring("refinement_".length())
                : path;
    }

    private static boolean refineAuthoritatively(ServerPlayer player, BlockPos pos, int forgeGrade,
                                                  String activatedKey, String recipeId) {
        boolean ok = ArtifactRefinementService.refine(player, recipeId, forgeGrade);
        if (ok) {
            ServerLevel serverLevel = player.serverLevel();
            serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    12 + forgeGrade * 2, 0.35D, 0.25D, 0.35D, 0.01D);
            player.displayClientMessage(Component.translatable(activatedKey), true);
        }
        return ok;
    }
}
